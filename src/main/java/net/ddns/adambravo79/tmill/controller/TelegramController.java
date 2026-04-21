package net.ddns.adambravo79.tmill.controller;

import lombok.extern.log4j.Log4j2;
import net.ddns.adambravo79.tmill.client.BloggerClient;
import net.ddns.adambravo79.tmill.model.MovieRecord;
import net.ddns.adambravo79.tmill.service.AudioPipelineService;
import net.ddns.adambravo79.tmill.service.MovieService;
import net.ddns.adambravo79.tmill.service.TranscricaoCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.objects.message.Message;

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
    private final BloggerClient bloggerClient;
    private final TranscricaoCache transcricaoCache;

    @Value("${t1000.features.transcription-enabled:false}")
    private boolean transcriptionEnabled;

    @Value("${blogger.blog-id}")
    private String blogId;

    @Value("${telegram.owner-id}")
    private long ownerId;

    public TelegramController(@Value("${bot.token}") String botToken,
            MovieService movieService,
            AudioPipelineService orchestrationService,
            BloggerClient bloggerClient,
            TranscricaoCache transcricaoCache) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.movieService = movieService;
        this.orchestrationService = orchestrationService;
        this.bloggerClient = bloggerClient;
        this.transcricaoCache = transcricaoCache;
    }

    /**
     * PONTO DE ENTRADA: O Telegram chama este método para cada nova interação.
     */
    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            processarCliqueBotao(update.getCallbackQuery());
            return;
        }
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

        // 2. Comando de Busca de Filme
        if (message.hasText() && message.getText().toLowerCase().startsWith("t1000 buscar ")) {
            String busca = message.getText().substring(13).trim();
            log.info("Iniciando busca para: {}", busca);

            var buscaBruta = movieService.buscarFilme(busca);

            if (buscaBruta == null || buscaBruta.results().isEmpty()) {
                enviarResposta(chatId, "❌ Não encontrei nada com esse nome.");
                return;
            }

            if (buscaBruta.results().size() > 1) {
                var resultadosOrdenados = buscaBruta.results().stream()
                        .sorted((f1, f2) -> {
                            String data1 = f1.releaseDate() != null ? f1.releaseDate() : "0000";
                            String data2 = f2.releaseDate() != null ? f2.releaseDate() : "0000";
                            int compData = data1.compareTo(data2);
                            if (compData != 0)
                                return compData;
                            return Double.compare(f2.popularity(), f1.popularity());
                        })
                        .toList();
                enviarOpcoesDesambiguacao(chatId, resultadosOrdenados);
            } else {
                var resposta = movieService.buscarPorId(buscaBruta.results().get(0).id());
                enviarFotoComLegenda(chatId, resposta.urlFoto(), resposta.textoFormatado());
            }
        }
    }

    // -------------------------------------------------------------------------
    // CALLBACK: Processa cliques em todos os botões inline
    // -------------------------------------------------------------------------

    private void processarCliqueBotao(CallbackQuery cb) {
        String data = cb.getData();
        long chatId = cb.getMessage().getChatId();

        // Botão de desambiguação de filmes (ex: "id:12345")
        if (data.startsWith("id:")) {
            long idFilme = Long.parseLong(data.replace("id:", ""));
            log.info("Botão clicado! Buscando detalhes do ID: {}", idFilme);
            var resposta = movieService.buscarPorId(idFilme);
            enviarFotoComLegenda(chatId, resposta.urlFoto(), resposta.textoFormatado());
            return;
        }

        // Botão "Publicar como Rascunho no Blogger"
        if (data.equals("blogger:publicar")) {
            processarPublicacaoBlogger(chatId);
            return;
        }

        // Botão "Não publicar"
        if (data.equals("blogger:cancelar")) {
            transcricaoCache.remover(chatId);
            enviarResposta(chatId, "👍 Ok! Transcrição descartada.");
        }
    }

    // -------------------------------------------------------------------------
    // LÓGICA DO BLOGGER
    // -------------------------------------------------------------------------

    /**
     * Chamado quando o usuário clica em "Publicar como Rascunho".
     * Recupera o texto do cache e envia para o BloggerClient.
     */
    private void processarPublicacaoBlogger(long chatId) {
        String texto = transcricaoCache.recuperar(chatId);

        if (texto == null || texto.isBlank()) {
            enviarResposta(chatId, "⚠️ Não encontrei nenhuma transcrição para publicar.");
            return;
        }

        enviarResposta(chatId, "⏳ Publicando rascunho no Blogger...");

        // Gera um título automático com as primeiras palavras da transcrição
        String titulo = gerarTituloAutomatico(texto);

        String urlRascunho = bloggerClient.criarRascunho(titulo, texto);

        if (urlRascunho != null) {
            transcricaoCache.remover(chatId); // Limpa o cache após publicar
            enviarResposta(chatId,
                    "✅ *Rascunho publicado com sucesso!*\n\n" +
                            "📝 *Título:* " + titulo + "\n" +
                            "🔗 [Ver Rascunhos](https://www.blogger.com/blogger.g?blogID=" + blogId
                            + "#allposts/postStatus=DRAFT)");
        } else {
            enviarResposta(chatId, "❌ Falha ao publicar o rascunho. Verifique os logs.");
        }
    }

    /**
     * Gera um título com as primeiras ~6 palavras da transcrição.
     */
    private String gerarTituloAutomatico(String texto) {
        String[] palavras = texto.trim().split("\\s+");
        int limite = Math.min(palavras.length, 6);
        String titulo = String.join(" ", java.util.Arrays.copyOf(palavras, limite));
        if (palavras.length > 6)
            titulo += "...";
        return titulo;
    }

    /**
     * Envia a mensagem do texto refinado COM os botões de ação do Blogger.
     */
    private void enviarRespostaComBotoesBlogger(long chatId, String texto) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(InlineKeyboardButton.builder()
                .text("📝 Publicar como Rascunho")
                .callbackData("blogger:publicar")
                .build());
        row.add(InlineKeyboardButton.builder()
                .text("❌ Não publicar")
                .callbackData("blogger:cancelar")
                .build());
        rows.add(row);

        SendMessage sm = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(texto)
                .parseMode("Markdown")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build())
                .build();

        try {
            telegramClient.execute(sm);
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem com botões do Blogger", e);
            // Fallback sem botões
            enviarResposta(chatId, texto);
        }
    }

    // -------------------------------------------------------------------------
    // ÁUDIO
    // -------------------------------------------------------------------------

    private void tratarFluxoAudio(Message message, long chatId) {
        if (!transcriptionEnabled) {
            enviarResposta(chatId, "🔇 Transcrição desativada.");
            return;
        }

        String fileId = message.hasVoice()
                ? message.getVoice().getFileId()
                : message.getAudio().getFileId();

        Optional<File> fileOpt = baixarArquivoTelegram(fileId);

        if (fileOpt.isEmpty()) {
            enviarResposta(chatId, "⚠️ Não consegui baixar o áudio.");
            return;
        }

        // BiConsumer: (textoMensagem, isUltimaMensagem)
        // Na última mensagem (texto refinado), enviamos COM os botões do Blogger.
        long userId = message.getFrom().getId();
        boolean isOwner = userId == ownerId;

        orchestrationService.processarFluxoAudio(fileOpt.get(), chatId, (texto, isUltima) -> {
            if (isUltima && isOwner) {
                enviarRespostaComBotoesBlogger(chatId, texto);
            } else {
                enviarResposta(chatId, texto);
            }
        });
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

    // -------------------------------------------------------------------------
    // FILMES — sem alteração
    // -------------------------------------------------------------------------

    private void enviarOpcoesDesambiguacao(long chatId, List<MovieRecord> resultados) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        var listaLimitada = resultados.stream().limit(10).toList();

        for (int i = 0; i < listaLimitada.size(); i++) {
            MovieRecord filme = listaLimitada.get(i);
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

    // -------------------------------------------------------------------------
    // MÉTODOS DE ENVIO
    // -------------------------------------------------------------------------

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
                log.error("Falha total ao enviar mensagem para chatId={}", chatId, ex);
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