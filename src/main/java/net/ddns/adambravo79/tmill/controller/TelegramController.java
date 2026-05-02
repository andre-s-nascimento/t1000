/* (c) 2026 | 02/05/2026 */
package net.ddns.adambravo79.tmill.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
import net.ddns.adambravo79.tmill.cache.TranscricaoCache;
import net.ddns.adambravo79.tmill.client.BloggerClient;
import net.ddns.adambravo79.tmill.model.MovieRecord;
import net.ddns.adambravo79.tmill.service.*;
import net.ddns.adambravo79.tmill.telegram.core.TelegramFacade;
import net.ddns.adambravo79.tmill.telegram.core.TelegramSafeExecutor;

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

    @Value("${t1000.audio.max-size-mb:20}")
    private int maxAudioSizeMb;

    @Value("${telegram.message.limit:4000}")
    private int telegramMessageLimit;

    private static final long MAX_AUDIO_SIZE_BYTES =
            20 * 1024 * 1024; // fallback, mas será substituído

    // Cache para evitar processamento duplicado do mesmo áudio em grupos
    private final Set<String> processedGroupAudios = ConcurrentHashMap.newKeySet();

    // Cache para tokens curtos (callback data)
    private final Map<String, AudioRequest> pendingGroupAudio = new ConcurrentHashMap<>();

    record AudioRequest(String fileId, long groupId) {}

    // =========================
    // 🚀 ENTRYPOINT
    // =========================

    @Override
    public void consume(List<Update> updates) {
        if (updates == null || updates.isEmpty()) return;
        log.info("📩 Recebidos {} updates do Telegram", updates.size());
        updates.parallelStream().forEach(this::processarUpdate);
    }

    public void consume(Update update) {
        if (update != null) {
            log.info("📩 Recebido update único do Telegram");
            processarUpdate(update);
        }
    }

    // =========================
    // 🧠 CORE ROUTER
    // =========================

    private void processarUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            log.info(
                    "🔘 Callback recebido chatId={} data={}",
                    chatId,
                    update.getCallbackQuery().getData());
            safeExecutor.run(
                    chatId,
                    telegramFacade::enviarMensagem,
                    () -> tratarCallback(update.getCallbackQuery()));
            return;
        }

        if (!update.hasMessage()) return;

        Message message = update.getMessage();
        long chatId = message.getChatId();
        long userId = message.getFrom().getId();

        String tipoMensagem;
        if (message.hasText()) {
            tipoMensagem = "texto";
        } else if (message.hasVoice()) {
            tipoMensagem = "voz";
        } else if (message.hasAudio()) {
            tipoMensagem = "áudio";
        } else {
            tipoMensagem = "outro";
        }

        log.info("💬 Mensagem recebida chatId={} userId={} tipo={}", chatId, userId, tipoMensagem);

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
        String texto = message.getText().toLowerCase().trim();
        log.debug("🔎 Processando texto chatId={} texto='{}'", chatId, texto);

        if (texto.equals("/start")) {
            enviarBoasVindas(chatId, message.getFrom().getFirstName());
            return;
        }

        if (!texto.startsWith("t1000 buscar")) return;

        String nome = texto.replace("t1000 buscar", "").trim();

        if (nome.length() < 3) {
            telegramFacade.enviarMensagem(
                    chatId, "🔍 O termo de busca deve ter pelo menos 3 caracteres.");
            return;
        }
        if (nome.length() > 100) {
            telegramFacade.enviarMensagem(
                    chatId, "🔍 O termo de busca é muito longo (máx. 100 caracteres).");
            return;
        }

        var busca = movieService.buscarFilme(nome);

        if (busca == null || busca.results() == null || busca.results().isEmpty()) {
            log.warn("⚠️ Filme não encontrado chatId={} query='{}'", chatId, nome);
            telegramFacade.enviarMensagem(chatId, "❌ Filme não encontrado.");
            return;
        }

        if (busca.results().size() == 1) {
            var id = busca.results().get(0).id();
            var resposta = movieService.buscarPorId(id);
            log.info("✅ Filme encontrado único chatId={} movieId={}", chatId, id);
            telegramFacade.enviarFoto(chatId, resposta.urlFoto(), resposta.textoFormatado());
            return;
        }

        log.info(
                "🧐 Vários resultados encontrados chatId={} total={}",
                chatId,
                busca.results().size());
        enviarOpcoesDesambiguacao(chatId, busca.results());
    }

    private void enviarBoasVindas(long chatId, String firstName) {
        String saudacao =
                String.format(
                        """
            🤖 Olá, %s! Eu sou o **Tmill Bot**, o robô de metal líquido das transcrições e buscas.

            📌 **O que posso fazer?**
            🎬 Buscar filmes: `t1000 buscar <nome do filme>`
            🎙️ Transcrever áudios: envie uma mensagem de voz ou áudio.

            💡 **Em grupos/canais:**
            Ao enviar um áudio, aparecerão botões para você escolher a transcrição **bruta** (🎙️) ou **refinada** (✨). O resultado chegará no seu **chat privado**.

            Desenvolvido com 🧠 e ☕ Java 21 + Spring Boot.
            """,
                        firstName);
        telegramFacade.enviarMensagem(chatId, saudacao);
    }

    // =========================
    // 🎙️ AUDIO
    // =========================

    private void tratarFluxoAudio(Message message, long chatId) {
        if (!transcriptionEnabled) {
            log.warn("⚠️ Transcrição desativada chatId={}", chatId);
            telegramFacade.enviarMensagem(chatId, "🔇 Transcrição desativada.");
            return;
        }

        String fileId =
                message.hasVoice()
                        ? message.getVoice().getFileId()
                        : message.getAudio().getFileId();
        long fileSize =
                message.hasVoice()
                        ? message.getVoice().getFileSize()
                        : message.getAudio().getFileSize();
        long maxBytes = maxAudioSizeMb * 1024L * 1024L;

        if (fileSize > maxBytes) {
            log.warn(
                    "⚠️ Áudio muito grande chatId={} size={} bytes (limite {} MB)",
                    chatId,
                    fileSize,
                    maxAudioSizeMb);
            telegramFacade.enviarMensagem(
                    chatId,
                    "📂 O arquivo de áudio excede "
                            + maxAudioSizeMb
                            + " MB. Envie um arquivo menor.");
            return;
        }

        if (fileId == null || fileId.isEmpty()) {
            log.error("❌ fileId inválido chatId={}", chatId);
            telegramFacade.enviarMensagem(
                    chatId, "❌ Identificador do áudio inválido. Tente novamente.");
            return;
        }

        boolean isGroup = isGroupChat(message);

        if (isGroup) {
            log.info("🎙️ Áudio recebido em grupo chatId={} fileId={}", chatId, fileId);
            enviarBotoesTranscricaoEmGrupo(chatId, fileId);
            return;
        }

        // Comportamento original para chat privado
        long userId = message.getFrom().getId();
        boolean isOwner = userId == ownerId;

        File file = fileService.baixarArquivo(fileId);

        audioService.processarFluxoAudio(
                file,
                chatId,
                (texto, isUltima) -> {
                    log.debug(
                            "📄 Texto transcrito chatId={} size={} ultima={}",
                            chatId,
                            texto.length(),
                            isUltima);
                    if (texto.length() > telegramMessageLimit) {
                        dividirMensagem(texto, telegramMessageLimit).stream()
                                .map(this::fecharMarkdown)
                                .forEach(parte -> telegramFacade.enviarMensagem(chatId, parte));
                        return;
                    }

                    if (Boolean.TRUE.equals(isUltima) && isOwner) {
                        log.info("📝 Enviando resposta com botões Blogger chatId={}", chatId);
                        enviarRespostaComBotoesBlogger(chatId, texto);
                    } else {
                        telegramFacade.enviarMensagem(chatId, texto);
                    }
                });
    }

    private boolean isGroupChat(Message message) {
        var chat = message.getChat();
        return chat.isGroupChat() || chat.isSuperGroupChat() || chat.isChannelChat();
    }

    private void enviarBotoesTranscricaoEmGrupo(long groupId, String fileId) {
        // Gera token único e curto (máx 20 caracteres)
        String token =
                Long.toHexString(System.nanoTime())
                        + Integer.toHexString(fileId.hashCode() & 0xFFFF);
        if (token.length() > 20) token = token.substring(0, 20);

        pendingGroupAudio.put(token, new AudioRequest(fileId, groupId));

        String brutoData = "trans_bruto|" + token;
        String refinadoData = "trans_refinado|" + token;

        InlineKeyboardMarkup markup =
                InlineKeyboardMarkup.builder()
                        .keyboard(
                                List.of(
                                        new InlineKeyboardRow(
                                                InlineKeyboardButton.builder()
                                                        .text("🎙️ Transcrição Bruta")
                                                        .callbackData(brutoData)
                                                        .build(),
                                                InlineKeyboardButton.builder()
                                                        .text("✨ Transcrição Refinada")
                                                        .callbackData(refinadoData)
                                                        .build())))
                        .build();

        String mensagem =
                "🎧 Áudio recebido! Clique num botão abaixo para receber a transcrição no seu chat"
                        + " privado.";
        telegramFacade.enviarComBotoes(groupId, mensagem, markup);
    }

    // =========================
    // 🔘 CALLBACK
    // =========================

    private void tratarCallback(CallbackQuery cb) {
        String data = cb.getData();
        long chatId = cb.getMessage().getChatId();
        log.debug("🔘 Tratando callback chatId={} data={}", chatId, data);

        if (data.startsWith("trans_bruto|") || data.startsWith("trans_refinado|")) {
            tratarCallbackTranscricaoComToken(cb, data);
            return;
        }

        if (data.startsWith("id:")) {
            long id = Long.parseLong(data.replace("id:", ""));
            var resposta = movieService.buscarPorId(id);
            log.info("✅ Callback de filme chatId={} movieId={}", chatId, id);
            telegramFacade.enviarFoto(chatId, resposta.urlFoto(), resposta.textoFormatado());
            return;
        }

        if ("blogger:cancelar".equals(data)) {
            cache.remover(chatId);
            log.info("🛑 Publicação cancelada chatId={}", chatId);
            telegramFacade.enviarMensagem(chatId, "❌ Publicação cancelada.");
            return;
        }

        if ("blogger:publicar".equals(data)) {
            String texto = cache.recuperar(chatId);
            if (texto == null) {
                log.warn("⚠️ Nenhuma transcrição disponível para publicar chatId={}", chatId);
                telegramFacade.enviarMensagem(chatId, "⚠️ Nenhuma transcrição disponível.");
                return;
            }
            String url = bloggerClient.criarRascunho("Post automático", texto);
            if (url != null) {
                log.info("✅ Publicação realizada chatId={} url={}", chatId, url);
                telegramFacade.enviarMensagem(chatId, "✅ Publicado: " + url);
                cache.remover(chatId);
            } else {
                log.error("❌ Falha ao publicar no Blogger chatId={}", chatId);
                telegramFacade.enviarMensagem(chatId, "❌ Falha ao publicar.");
            }
        }
    }

    private void tratarCallbackTranscricaoComToken(CallbackQuery callback, String data) {
        String[] parts = data.split("\\|", 2);
        if (parts.length < 2) {
            log.error("Callback malformado: {}", data);
            return;
        }
        String tipo = parts[0];
        String token = parts[1];

        AudioRequest request = pendingGroupAudio.remove(token);
        if (request == null) {
            log.warn("Token inválido ou expirado: {}", token);
            telegramFacade.answerCallbackQuery(
                    callback.getId(), "Este pedido expirou. Envie o áudio novamente.", true);
            return;
        }

        String fileId = request.fileId();
        long groupId = request.groupId();
        long userId = callback.getFrom().getId();
        int messageId = callback.getMessage().getMessageId();

        String key = groupId + "_" + fileId;
        if (processedGroupAudios.contains(key)) {
            log.info("Áudio já processado no grupo groupId={} fileId={}", groupId, fileId);
            telegramFacade.answerCallbackQuery(
                    callback.getId(), "Este áudio já foi transcrito por outro usuário.", true);
            return;
        }

        processedGroupAudios.add(key);
        telegramFacade.answerCallbackQuery(
                callback.getId(), "Processando áudio... enviarei no privado.", false);

        CompletableFuture.runAsync(
                () -> {
                    try {
                        File audioFile = fileService.baixarArquivo(fileId);
                        final String[] resultado = {null};
                        final boolean[] refinadoRecebido = {false};

                        audioService.processarFluxoAudio(
                                audioFile,
                                userId,
                                (texto, isUltima) -> {
                                    if ("trans_bruto".equals(tipo)) {
                                        if (!isUltima) resultado[0] = texto;
                                    } else {
                                        if (isUltima) {
                                            resultado[0] = texto;
                                            refinadoRecebido[0] = true;
                                        }
                                    }
                                });

                        if (resultado[0] == null) {
                            throw new IllegalStateException(
                                    "Nenhum texto produzido para tipo " + tipo);
                        }

                        String prefixo =
                                "trans_bruto".equals(tipo)
                                        ? "🎙️ *Transcrição Bruta:*\n"
                                        : "✨ *Transcrição Refinada:*\n";
                        String mensagemFinal = prefixo + resultado[0];
                        if (mensagemFinal.length() > telegramMessageLimit) {
                            List<String> partes =
                                    dividirMensagem(mensagemFinal, telegramMessageLimit);
                            for (String parte : partes) {
                                telegramFacade.enviarMensagem(userId, parte);
                            }
                        } else {
                            telegramFacade.enviarMensagem(userId, mensagemFinal);
                        }

                        telegramFacade.editarMensagem(
                                groupId, messageId, "✅ Transcrição enviada no privado.");

                    } catch (Exception e) {
                        log.error(
                                "Erro ao processar áudio para grupo groupId={} fileId={}",
                                groupId,
                                fileId,
                                e);
                        telegramFacade.editarMensagem(
                                groupId, messageId, "❌ Falha ao processar áudio. Tente novamente.");
                        telegramFacade.enviarMensagem(
                                userId,
                                "❌ Ocorreu um erro ao processar seu áudio. Motivo: "
                                        + e.getMessage());
                    }
                });
    }

    private void editarMensagemGrupo(long chatId, int messageId, String novoTexto) {
        telegramFacade.editarMensagem(chatId, messageId, novoTexto);
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
        if (count % 2 != 0) return texto + "*";
        return texto;
    }
}
