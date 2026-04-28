/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.telegram;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@FunctionalInterface
public interface TelegramAction {
    void run() throws TelegramApiException;
}
