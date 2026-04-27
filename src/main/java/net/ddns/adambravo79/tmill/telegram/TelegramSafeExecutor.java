/* (c) 2026 */
package net.ddns.adambravo79.tmill.telegram;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TelegramSafeExecutor {

    public void run(Long chatId, TelegramSender fallback, TelegramAction action) {
        try {
            action.run();

        } catch (TelegramApiException e) {
            log.error("telegram_error chatId={} msg={}", chatId, e.getMessage(), e);

            try {
                fallback.enviar(chatId, "⚠️ Erro ao enviar mensagem. Tente novamente.");
            } catch (Exception fallbackError) {
                log.error("fallback_error chatId={}", chatId, fallbackError);
            }

        } catch (Exception e) {
            log.error("unexpected_error chatId={}", chatId, e);

            try {
                fallback.enviar(chatId, "⚠️ Erro inesperado.");
            } catch (Exception fallbackError) {
                log.error("fallback_error chatId={}", chatId, fallbackError);
            }
        }
    }
}
