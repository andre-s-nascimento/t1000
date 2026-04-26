/* (c) 2026 */
package net.ddns.adambravo79.tmill.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.RequiredArgsConstructor;
import net.ddns.adambravo79.tmill.telegram.TelegramFileException;

@Service
@RequiredArgsConstructor
public class TelegramFileService {

  private final TelegramClient telegramClient;

  public File baixarArquivo(String fileId) {
    try {
      var telegramFile = telegramClient.execute(new GetFile(fileId));
      File localTemp = telegramClient.downloadFile(telegramFile);

      File finalFile = new File("temp_audio/" + localTemp.getName() + ".oga");
      Files.createDirectories(finalFile.getParentFile().toPath());
      Files.move(localTemp.toPath(), finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

      return finalFile;

    } catch (Exception e) {
      throw new TelegramFileException("Erro ao baixar arquivo do Telegram", e);
    }
  }
}
