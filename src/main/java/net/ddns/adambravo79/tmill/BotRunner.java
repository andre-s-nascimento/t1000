package net.ddns.adambravo79.tmill;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.ddns.adambravo79.tmill.controller.TelegramController;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Log4j2
@Component
@RequiredArgsConstructor
public class BotRunner implements CommandLineRunner {

    private final TelegramController telegramController;
    private final String botToken;

    @Override
    public void run(String... args) throws Exception {
        log.info("🤖 Iniciando registro manual do bot T1000...");

        try {
            // Criamos a aplicação sem o try-with-resources para ela não fechar
            TelegramBotsLongPollingApplication botApp = new TelegramBotsLongPollingApplication();

            // Registra o Controller
            botApp.registerBot(botToken, telegramController);

            log.info("🚀 T1000 operacional e aguardando comandos!");

            // Mantém a thread principal viva para o CommandLineRunner não encerrar
            Thread.currentThread().join();
        } catch (Exception e) {
            log.error("❌ Falha crítica ao registrar o bot. ", e);

        }
    }
}