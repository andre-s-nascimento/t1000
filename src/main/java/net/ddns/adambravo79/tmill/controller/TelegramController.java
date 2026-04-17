package net.ddns.adambravo79.tmill.controller;

import lombok.extern.log4j.Log4j2;
import net.ddns.adambravo79.tmill.model.MovieRecord;
import net.ddns.adambravo79.tmill.service.MovieService;
import net.ddns.adambravo79.tmill.service.AudioPipelineService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Component
public class TelegramController implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final MovieService movieService;
    private final AudioPipelineService orchestrationService;

    @Value("${t1000.features.transcription-enabled:false}")
    private boolean transcriptionEnabled;

    public TelegramController(@Value("${bot.token}") String botToken,
            MovieService movieService,
            AudioPipelineService orchestrationService) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.movieService = movieService;
        this.orchestrationService = orchestrationService;
    }

    /**
     * PONTO DE ENTRADA: O Telegram chama este método para cada nova interação.
     */
    @Override
    public void consume(Update update) {
        // Se o usuário CLICOU em um botão, o Update contém um CallbackQuery.
        if (update.hasCallbackQuery()) {
            processarCliqueBotao(update.getCallbackQuery());
            return;
        }

        // Se o usuário ENVIOU uma mensagem (texto ou áudio).
        if (update.hasMessage()) {
            processarUpdate(update);
        }
    }

    private void processarUpdate(Update update) {
        var message = update.getMessage();
        long chatId = message.getChatId();

        // 1. Processamento de Voz
        if (message.hasVoice() || message.hasAudio()) {
            tratarFluxoAudio(message, chatId);
            return;
        }

        // 2. Comando de Busca: "t1000 buscar [nome]"
        if (message.hasText() && message.getText().toLowerCase().startsWith("t1000 buscar ")) {
            String busca = message.getText().substring(13).trim();
            log.info("Iniciando busca para: {}", busca);

            var buscaBruta = movieService.buscarFilme(busca);

            if (buscaBruta == null || buscaBruta.results().isEmpty()) {
                enviarResposta(chatId, "❌ Não encontrei nada com esse nome.");
                return;
            }

            // Se o TMDB retornou mais de 1 resultado, chamamos a desambiguação.
            if (buscaBruta.results().size() > 1) {
                // Ordenamos a lista antes de enviar para a desambiguação
                var resultadosOrdenados = buscaBruta.results().stream()
                        .sorted((f1, f2) -> {// Primeiro critério: Ano (Crescente como você pediu)
                            String data1 = f1.releaseDate() != null ? f1.releaseDate() : "0000";
                            String data2 = f2.releaseDate() != null ? f2.releaseDate() : "0000";
                            int compData = data1.compareTo(data2);

                            if (compData != 0)
                                return compData;

                            // Segundo critério (Desempate): Popularidade (Quem é mais famoso ganha)
                            return Double.compare(f2.popularity(), f1.popularity());
                        })
                        .toList();

                enviarOpcoesDesambiguacao(chatId, resultadosOrdenados);
            } else {
                // Se só tem 1, traz os detalhes e a foto direto.
                var resposta = movieService.buscarPorId(buscaBruta.results().get(0).id());
                enviarFotoComLegenda(chatId, resposta.urlFoto(), resposta.textoFormatado());
            }
        }
    }

    private void enviarOpcoesDesambiguacao(long chatId, List<MovieRecord> resultados) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        // Limitamos aos 10 primeiros JÁ ORDENADOS
        var listaLimitada = resultados.stream().limit(10).toList();

        for (int i = 0; i < listaLimitada.size(); i++) {
            MovieRecord filme = listaLimitada.get(i);

            // Trata o ano para exibição no botão
            String ano = (filme.releaseDate() != null && filme.releaseDate().length() >= 4)
                    ? " (" + filme.releaseDate().substring(0, 4) + ")"
                    : " (S/A)";

            InlineKeyboardButton botao = InlineKeyboardButton.builder()
                    .text(filme.title() + ano)
                    .callbackData("id:" + filme.id())
                    .build();

            currentRow.add(botao);

            if ((i + 1) % 2 == 0 || (i + 1) == listaLimitada.size()) {
                rows.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }

        SendMessage sm = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("🧐 Encontrei vários resultados (ordenados por ano). Qual deles você quer?")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build())
                .build();

        try {
            telegramClient.execute(sm);
        } catch (Exception e) {
            log.error("Erro ao renderizar teclado ordenado", e);
        }
    }

    /**
     * LÓGICA DE REAÇÃO: Processa o ID que veio no clique do botão.
     */
    private void processarCliqueBotao(CallbackQuery cb) {
        String data = cb.getData();
        long chatId = cb.getMessage().getChatId();

        // Se o dado começar com "id:", sabemos que é um ID do TMDB.
        if (data.startsWith("id:")) {
            long idFilme = Long.parseLong(data.replace("id:", ""));
            log.info("Botão clicado! Buscando detalhes do ID: {}", idFilme);

            // Agora buscamos os detalhes exatos pelo ID, garantindo a foto correta.
            var resposta = movieService.buscarPorId(idFilme);
            enviarFotoComLegenda(chatId, resposta.urlFoto(), resposta.textoFormatado());
        }
    }

    // --- MÉTODOS AUXILIARES (Áudio, Envio de Foto, etc) ---

    private void tratarFluxoAudio(org.telegram.telegrambots.meta.api.objects.message.Message message, long chatId) {
        if (!transcriptionEnabled) {
            enviarResposta(chatId, "🔇 Transcrição desativada.");
            return;
        }
        String fileId = message.hasVoice() ? message.getVoice().getFileId() : message.getAudio().getFileId();

        Optional<File> fileOpt = baixarArquivoTelegram(fileId);

        if (fileOpt.isEmpty()) {
            enviarResposta(chatId, "⚠️ Não consegui baixar o áudio.");
            return;
        }

        File file = fileOpt.get();

        if (file != null) {
            orchestrationService.processarFluxoAudio(file, texto -> enviarResposta(chatId, texto));
        }
    }

    private Optional<File> baixarArquivoTelegram(String fileId) {
        try {
            var telegramFile = telegramClient.execute(new GetFile(fileId));
            File localTemp = telegramClient.downloadFile(telegramFile);

            File finalFile = new File("temp_audio/" + localTemp.getName() + ".oga");
            Files.createDirectories(finalFile.getParentFile().toPath());
            Files.move(localTemp.toPath(), finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return Optional.of(finalFile);

        } catch (Exception e) {
            log.error("Erro ao baixar arquivo do Telegram", e);
            return Optional.empty();
        }
    }

    private void enviarResposta(long chatId, String texto) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(texto)
                    .parseMode("Markdown")
                    .build());
        } catch (Exception e) {
            try {
                telegramClient.execute(new SendMessage(String.valueOf(chatId), texto));
            } catch (Exception ex) {
            }
        }
    }

    private void enviarFotoComLegenda(long chatId, String urlFoto, String legenda) {
        try {
            telegramClient.execute(SendPhoto.builder()
                    .chatId(String.valueOf(chatId))
                    .photo(new InputFile(urlFoto))
                    .caption(legenda)
                    .parseMode("Markdown")
                    .build());
        } catch (Exception e) {
            log.warn("Erro ao enviar foto, enviando apenas texto.");
            enviarResposta(chatId, legenda);
        }
    }
}