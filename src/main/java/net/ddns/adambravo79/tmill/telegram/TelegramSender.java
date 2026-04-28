/* (c) 2026-2026 */
package net.ddns.adambravo79.tmill.telegram;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@FunctionalInterface
public interface TelegramSender {
    void enviar(long chatId, String mensagem) throws TelegramApiException;
}
