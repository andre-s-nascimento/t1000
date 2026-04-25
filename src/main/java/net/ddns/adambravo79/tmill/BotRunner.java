/* (c) 2026 */
package net.ddns.adambravo79.tmill;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.controller.TelegramController;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotRunner implements CommandLineRunner {

  private final TelegramController telegramController;
  private final String botToken;

  @Override
  public void run(String... args) throws Exception {
    log.info("🤖 Iniciando registro manual do bot T1000...");

    // Criamos a aplicação sem o try-with-resources para ela não fechar
    @SuppressWarnings("resource")
    TelegramBotsLongPollingApplication botApp = new TelegramBotsLongPollingApplication();

    // Registra o Controller
    botApp.registerBot(botToken, telegramController);

    log.info("🚀 T1000 operacional e aguardando comandos!");

    // Mantém a thread principal viva para o CommandLineRunner não encerrar
    Thread.currentThread().join();
  }
}
