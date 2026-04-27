/* (c) 2026 */
package net.ddns.adambravo79.tmill.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.client.BloggerClient;
import net.ddns.adambravo79.tmill.model.MovieRecord;
import net.ddns.adambravo79.tmill.service.*;
import net.ddns.adambravo79.tmill.telegram.TelegramFacade;
import net.ddns.adambravo79.tmill.telegram.TelegramSafeExecutor;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramController implements LongPollingUpdateConsumer {

    private final MovieService movieService;
    private final AudioPipelineService audioService;
    private final BloggerClient bloggerClient;
    private final TranscricaoCache cache;
    private final TelegramFileService fileService;
    private final TelegramFacade telegramFacade;
    private final TelegramSafeExecutor safeExecutor;

    @Value("${t1000.features.transcription-enabled:false}")
    private boolean transcriptionEnabled;

    @Value("${telegram.owner.id:0}")
    private long ownerId;

    private static final int TELEGRAM_LIMIT = 4000;

    // =========================
    // 🚀 ENTRYPOINT
    // =========================

    @Override
    public void consume(List<Update> updates) {
        if (updates == null || updates.isEmpty()) return;
        updates.parallelStream().forEach(this::processarUpdate);
    }

    public void consume(Update update) {
        if (update != null) {
            processarUpdate(update);
        }
    }

    // =========================
    // 🧠 CORE ROUTER
    // =========================

    private void processarUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            safeExecutor.run(
                    chatId,
                    telegramFacade::enviarMensagem,
                    () -> tratarCallback(update.getCallbackQuery()));
            return;
        }

        if (!update.hasMessage()) return;

        Message message = update.getMessage();
        long chatId = message.getChatId();

        if (message.hasVoice() || message.hasAudio()) {
            safeExecutor.run(
                    chatId,
                    telegramFacade::enviarMensagem,
                    () -> tratarFluxoAudio(message, chatId));
            return;
        }

        if (message.hasText()) {
            safeExecutor.run(
                    chatId, telegramFacade::enviarMensagem, () -> tratarTexto(message, chatId));
        }
    }

    // =========================
    // 🎬 TEXTO
    // =========================

    private void tratarTexto(Message message, long chatId) {
        String texto = message.getText().toLowerCase();

        if (!texto.startsWith("t1000 buscar")) return;

        String nome = texto.replace("t1000 buscar", "").trim();

        var busca = movieService.buscarFilme(nome);

        if (busca == null || busca.results() == null || busca.results().isEmpty()) {
            telegramFacade.enviarMensagem(chatId, "❌ Filme não encontrado.");
            return;
        }

        if (busca.results().size() == 1) {
            var id = busca.results().get(0).id();
            var resposta = movieService.buscarPorId(id);

            telegramFacade.enviarFoto(chatId, resposta.urlFoto(), resposta.textoFormatado());
            return;
        }

        enviarOpcoesDesambiguacao(chatId, busca.results());
    }

    // =========================
    // 🎙️ AUDIO
    // =========================

    private void tratarFluxoAudio(Message message, long chatId) {
        if (!transcriptionEnabled) {
            telegramFacade.enviarMensagem(chatId, "🔇 Transcrição desativada.");
            return;
        }

        String fileId =
                message.hasVoice()
                        ? message.getVoice().getFileId()
                        : message.getAudio().getFileId();

        // ✅ Agora sem try/catch: exceções do fileService são lançadas e capturadas pelo
        // SafeExecutor
        File file = fileService.baixarArquivo(fileId);

        long userId = message.getFrom().getId();
        boolean isOwner = userId == ownerId;

        audioService.processarFluxoAudio(
                file,
                chatId,
                (texto, isUltima) -> {
                    if (texto.length() > TELEGRAM_LIMIT) {
                        dividirMensagem(texto, TELEGRAM_LIMIT).stream()
                                .map(this::fecharMarkdown)
                                .forEach(parte -> telegramFacade.enviarMensagem(chatId, parte));
                        return;
                    }

                    if (Boolean.TRUE.equals(isUltima) && isOwner) {
                        enviarRespostaComBotoesBlogger(chatId, texto);
                    } else {
                        telegramFacade.enviarMensagem(chatId, texto);
                    }
                });
    }

    // =========================
    // 🔘 CALLBACK
    // =========================

    private void tratarCallback(CallbackQuery cb) {
        String data = cb.getData();
        long chatId = cb.getMessage().getChatId();

        if (data.startsWith("id:")) {
            long id = Long.parseLong(data.replace("id:", ""));
            var resposta = movieService.buscarPorId(id);

            telegramFacade.enviarFoto(chatId, resposta.urlFoto(), resposta.textoFormatado());
            return;
        }

        if ("blogger:cancelar".equals(data)) {
            cache.remover(chatId);
            telegramFacade.enviarMensagem(chatId, "❌ Publicação cancelada.");
            return;
        }

        if ("blogger:publicar".equals(data)) {
            String texto = cache.recuperar(chatId);

            if (texto == null) {
                telegramFacade.enviarMensagem(chatId, "⚠️ Nenhuma transcrição disponível.");
                return;
            }

            String url = bloggerClient.criarRascunho("Post automático", texto);

            if (url != null) {
                telegramFacade.enviarMensagem(chatId, "✅ Publicado: " + url);
                cache.remover(chatId);
            } else {
                telegramFacade.enviarMensagem(chatId, "❌ Falha ao publicar.");
            }
        }
    }

    // =========================
    // 🧩 UI HELPERS
    // =========================

    private void enviarOpcoesDesambiguacao(long chatId, List<MovieRecord> resultados) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow current = new InlineKeyboardRow();

        var lista = resultados.stream().limit(10).toList();

        for (int i = 0; i < lista.size(); i++) {
            var filme = lista.get(i);

            String ano =
                    (filme.releaseDate() != null && filme.releaseDate().length() >= 4)
                            ? " (" + filme.releaseDate().substring(0, 4) + ")"
                            : " (S/A)";

            current.add(
                    InlineKeyboardButton.builder()
                            .text(filme.title() + ano)
                            .callbackData("id:" + filme.id())
                            .build());

            if ((i + 1) % 2 == 0 || (i + 1) == lista.size()) {
                rows.add(current);
                current = new InlineKeyboardRow();
            }
        }

        var markup = InlineKeyboardMarkup.builder().keyboard(rows).build();

        telegramFacade.enviarComBotoes(
                chatId, "🧐 Encontrei vários resultados. Qual você quer?", markup);
    }

    private void enviarRespostaComBotoesBlogger(long chatId, String texto) {
        cache.salvar(chatId, texto);

        InlineKeyboardMarkup markup =
                InlineKeyboardMarkup.builder()
                        .keyboard(
                                List.of(
                                        new InlineKeyboardRow(
                                                InlineKeyboardButton.builder()
                                                        .text("📝 Publicar")
                                                        .callbackData("blogger:publicar")
                                                        .build(),
                                                InlineKeyboardButton.builder()
                                                        .text("❌ Cancelar")
                                                        .callbackData("blogger:cancelar")
                                                        .build())))
                        .build();

        telegramFacade.enviarComBotoes(chatId, texto, markup);
    }

    // =========================
    // ✂️ UTIL
    // =========================

    private List<String> dividirMensagem(String texto, int limite) {
        List<String> partes = new ArrayList<>();

        while (texto.length() > limite) {
            int corte = texto.lastIndexOf(" ", limite);
            if (corte <= 0) corte = limite;

            partes.add(texto.substring(0, corte));
            texto = texto.substring(corte).trim();
        }

        if (!texto.isEmpty()) {
            partes.add(texto);
        }

        return partes;
    }

    private String fecharMarkdown(String texto) {
        long count = texto.chars().filter(c -> c == '*').count();
        if (count % 2 != 0) {
            return texto + "*";
        }
        return texto;
    }
}
