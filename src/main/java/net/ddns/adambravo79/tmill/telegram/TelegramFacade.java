/* (c) 2026 */
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

  // 🔹 fallback tipado corretamente
  private void enviarFallback(long chatId, String mensagem) throws TelegramApiException {
    var fallback = SendMessage.builder().chatId(String.valueOf(chatId)).text(mensagem).build();

    telegramClient.execute(fallback);
  }

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

  public org.telegram.telegrambots.meta.api.objects.File getFile(GetFile getFile)
      throws TelegramApiException {
    return telegramClient.execute(getFile);
  }

  public java.io.File downloadFile(org.telegram.telegrambots.meta.api.objects.File tgFile)
      throws TelegramApiException {
    return telegramClient.downloadFile(tgFile);
  }
}
