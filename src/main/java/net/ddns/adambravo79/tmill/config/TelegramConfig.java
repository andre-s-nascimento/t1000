/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class TelegramConfig {

    @Bean
    public TelegramClient telegramClient(@Value("${bot.token}") String botToken) {
        // ⚠️ Nunca logar o token completo por segurança
        log.info("🤖 Inicializando TelegramClient (token mascarado)");
        return new OkHttpTelegramClient(botToken);
    }
}
