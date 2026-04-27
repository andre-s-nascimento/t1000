/* (c) 2026-2026 */
package net.ddns.adambravo79.tmill.telegram;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Fachada para interação com a API do Telegram.
 *
 * <p>Principais responsabilidades: - Enviar mensagens de texto com suporte a Markdown. - Enviar
 * fotos com legenda. - Enviar mensagens com botões interativos. - Obter e baixar arquivos do
 * Telegram.
 *
 * <p>Utiliza {@link TelegramSafeExecutor} para garantir execução segura com fallback em caso de
 * falha.
 */
@Slf4j
@Component
public class TelegramFacade {

    private static final String MARKDOWN = "Markdown";

    private final TelegramClient telegramClient;
    private final TelegramSafeExecutor safeExecutor;

    public TelegramFacade(TelegramClient telegramClient, TelegramSafeExecutor safeExecutor) {
        this.telegramClient = telegramClient;
        this.safeExecutor = safeExecutor;
    }

    /** Envia mensagem de fallback sem parseMode, usada em caso de falha. */
    private void enviarFallback(long chatId, String mensagem) throws TelegramApiException {
        var fallback = SendMessage.builder().chatId(String.valueOf(chatId)).text(mensagem).build();

        telegramClient.execute(fallback);
    }

    /**
     * Envia uma mensagem de texto com suporte a Markdown.
     *
     * @param chatId identificador do chat.
     * @param texto conteúdo da mensagem.
     */
    public void enviarMensagem(long chatId, String texto) {
        safeExecutor.run(
                chatId,
                this::enviarFallback,
                () -> {
                    var msg =
                            SendMessage.builder()
                                    .chatId(String.valueOf(chatId))
                                    .text(texto)
                                    .parseMode(MARKDOWN)
                                    .build();

                    telegramClient.execute(msg);
                });
    }

    /**
     * Envia uma foto com legenda.
     *
     * @param chatId identificador do chat.
     * @param url URL da imagem.
     * @param legenda legenda da foto.
     */
    public void enviarFoto(long chatId, String url, String legenda) {
        safeExecutor.run(
                chatId,
                this::enviarFallback,
                () -> {
                    var photo =
                            SendPhoto.builder()
                                    .chatId(String.valueOf(chatId))
                                    .photo(new InputFile(url))
                                    .caption(legenda)
                                    .parseMode(MARKDOWN)
                                    .build();

                    telegramClient.execute(photo);
                });
    }

    /**
     * Envia uma mensagem com botões interativos.
     *
     * @param chatId identificador do chat.
     * @param texto conteúdo da mensagem.
     * @param markup teclado inline com botões.
     */
    public void enviarComBotoes(long chatId, String texto, InlineKeyboardMarkup markup) {
        safeExecutor.run(
                chatId,
                this::enviarFallback,
                () -> {
                    var msg =
                            SendMessage.builder()
                                    .chatId(String.valueOf(chatId))
                                    .text(texto)
                                    .replyMarkup(markup)
                                    .parseMode(MARKDOWN)
                                    .build();

                    telegramClient.execute(msg);
                });
    }

    /**
     * Obtém metadados de um arquivo do Telegram.
     *
     * @param getFile requisição de arquivo.
     * @return metadados do arquivo.
     * @throws TelegramApiException em caso de falha.
     */
    public org.telegram.telegrambots.meta.api.objects.File getFile(GetFile getFile)
            throws TelegramApiException {
        return telegramClient.execute(getFile);
    }

    /**
     * Faz o download de um arquivo do Telegram para o sistema local.
     *
     * @param tgFile metadados do arquivo.
     * @return arquivo baixado.
     * @throws TelegramApiException em caso de falha.
     */
    public java.io.File downloadFile(org.telegram.telegrambots.meta.api.objects.File tgFile)
            throws TelegramApiException {
        return telegramClient.downloadFile(tgFile);
    }
}
