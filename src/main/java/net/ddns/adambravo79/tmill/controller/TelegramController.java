/* (c) 2026 | 01/05/2026 */
package net.ddns.adambravo79.tmill.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

    private static final int TELEGRAM_LIMIT = 4000;

    // NOVO: limite máximo de tamanho de áudio (20 MB)
    private static final long MAX_AUDIO_SIZE_BYTES = 20 * 1024 * 1024; // 20 MB

    // NOVO: cache para evitar processamento duplicado do mesmo áudio em grupos
    private final Set<String> processedGroupAudios = ConcurrentHashMap.newKeySet();

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

        // NOVO: suporte ao comando /start
        if (texto.equals("/start")) {
            enviarBoasVindas(chatId, message.getFrom().getFirstName());
            return;
        }

        // Comando existente: buscar filmes
        if (!texto.startsWith("t1000 buscar")) return;

        String nome = texto.replace("t1000 buscar", "").trim();

        // NOVO: validação da busca (mínimo 3, máximo 100 caracteres)
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

    // NOVO: mensagem de boas-vindas com /start
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

        // NOVO: validação de tamanho do arquivo
        if (fileSize > MAX_AUDIO_SIZE_BYTES) {
            log.warn("⚠️ Áudio muito grande chatId={} size={} bytes", chatId, fileSize);
            telegramFacade.enviarMensagem(
                    chatId, "📂 O arquivo de áudio excede 20 MB. Envie um arquivo menor.");
            return;
        }

        if (fileId == null || fileId.isEmpty()) {
            log.error("❌ fileId inválido chatId={}", chatId);
            telegramFacade.enviarMensagem(
                    chatId, "❌ Identificador do áudio inválido. Tente novamente.");
            return;
        }

        boolean isGroup = isGroupChat(message);

        // NOVO: comportamento diferenciado para grupos
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
                    if (texto.length() > TELEGRAM_LIMIT) {
                        dividirMensagem(texto, TELEGRAM_LIMIT).stream()
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

    // NOVO: detecta se a mensagem veio de um grupo/supergrupo/canal
    private boolean isGroupChat(Message message) {
        var chat = message.getChat();
        return chat.isGroupChat() || chat.isSuperGroupChat() || chat.isChannelChat();
    }

    // NOVO: envia botões para transcrição em grupos
    private void enviarBotoesTranscricaoEmGrupo(long groupId, String fileId) {
        String brutoData = "trans_bruto|" + fileId + "|" + groupId;
        String refinadoData = "trans_refinado|" + fileId + "|" + groupId;

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

        // NOVO: callbacks de transcrição em grupo
        if (data.startsWith("trans_bruto|") || data.startsWith("trans_refinado|")) {
            tratarCallbackTranscricao(cb, data);
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

    // NOVO: processa clique nos botões de transcrição dentro de grupos
    private void tratarCallbackTranscricao(CallbackQuery callback, String data) {
        String[] parts = data.split("\\|", 3);
        if (parts.length < 3) {
            log.error("Callback malformado: {}", data);
            return;
        }
        String tipo = parts[0]; // "trans_bruto" ou "trans_refinado"
        String fileId = parts[1];
        long groupId = Long.parseLong(parts[2]);
        long userId = callback.getFrom().getId();
        int messageId = callback.getMessage().getMessageId();

        String key = groupId + "_" + fileId;
        if (processedGroupAudios.contains(key)) {
            log.info("Áudio já processado no grupo groupId={} fileId={}", groupId, fileId);
            telegramFacade.answerCallbackQuery(
                    callback.getId(), "Este áudio já foi transcrito por outro usuário.", true);
            return;
        }

        // Marca como processado imediatamente para evitar duplicatas concorrentes
        processedGroupAudios.add(key);

        // Responde ao clique rapidamente
        telegramFacade.answerCallbackQuery(
                callback.getId(), "Processando áudio... enviarei no privado.", false);

        // Processa de forma assíncrona para não bloquear o callback
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
                                        // Para bruto, a primeira mensagem (não refinada) é a que
                                        // interessa
                                        if (!isUltima) {
                                            resultado[0] = texto;
                                        }
                                    } else {
                                        // Para refinado, capturamos a última mensagem (refinada)
                                        if (isUltima) {
                                            resultado[0] = texto;
                                            refinadoRecebido[0] = true;
                                        }
                                    }
                                });

                        if (resultado[0] == null) {
                            throw new IllegalStateException(
                                    "Nenhum texto foi produzido para o tipo " + tipo);
                        }

                        // Envia a transcrição para o privado do usuário
                        String prefixo =
                                "trans_bruto".equals(tipo)
                                        ? "🎙️ *Transcrição Bruta:*\n"
                                        : "✨ *Transcrição Refinada:*\n";
                        telegramFacade.enviarMensagem(userId, prefixo + resultado[0]);

                        // Edita a mensagem original do grupo removendo os botões
                        editarMensagemGrupo(
                                groupId, messageId, "✅ Transcrição enviada no privado.");

                    } catch (Exception e) {
                        log.error(
                                "Erro ao processar áudio para grupo groupId={} fileId={}",
                                groupId,
                                fileId,
                                e);
                        editarMensagemGrupo(
                                groupId, messageId, "❌ Falha ao processar áudio. Tente novamente.");
                        // Em caso de falha, não removemos do cache para evitar repetição? Melhor
                        // manter para
                        // não spammar.
                        // Mas poderia remover se quiser dar chance de tentar de novo. Por ora
                        // mantemos
                        // bloqueado.
                    }
                });
    }

    // NOVO: edita uma mensagem existente no grupo (remove botões)
    private void editarMensagemGrupo(long chatId, int messageId, String novoTexto) {
        telegramFacade.editarMensagem(chatId, messageId, novoTexto);
    }

    // =========================
    // 🧩 UI HELPERS
    // =========================

    private void enviarOpcoesDesambiguacao(long chatId, List<MovieRecord> resultados) {
        log.debug(
                "🧩 Enviando opções de desambiguação chatId={} total={}",
                chatId,
                resultados.size());

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

        log.info(
                "🧩 Opções de desambiguação enviadas chatId={} totalOpcoes={}",
                chatId,
                lista.size());
        telegramFacade.enviarComBotoes(
                chatId, "🧐 Encontrei vários resultados. Qual você quer?", markup);
    }

    private void enviarRespostaComBotoesBlogger(long chatId, String texto) {
        cache.salvar(chatId, texto);
        log.info(
                "📝 Cache salvo para publicação Blogger chatId={} textoSize={}",
                chatId,
                texto.length());

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

        log.info("📝 Botões de publicação enviados chatId={}", chatId);
        telegramFacade.enviarComBotoes(chatId, texto, markup);
    }

    // =========================
    // ✂️ UTIL
    // =========================

    private List<String> dividirMensagem(String texto, int limite) {
        log.debug("✂️ Dividindo mensagem size={} limite={}", texto.length(), limite);

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

        log.debug("✂️ Mensagem dividida em {} partes", partes.size());
        return partes;
    }

    private String fecharMarkdown(String texto) {
        long count = texto.chars().filter(c -> c == '*').count();
        if (count % 2 != 0) {
            log.debug("✏️ Ajustando markdown textoSize={} asteriscos={}", texto.length(), count);
            return texto + "*";
        }
        return texto;
    }
}
