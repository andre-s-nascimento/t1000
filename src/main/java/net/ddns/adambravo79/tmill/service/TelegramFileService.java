/* (c) 2026 */
package net.ddns.adambravo79.tmill.service;

import java.io.File;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.exception.TelegramFileException;
import net.ddns.adambravo79.tmill.telegram.TelegramFacade;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramFileService {

  private final TelegramFacade telegramFacade;

  /**
   * Baixa um arquivo do Telegram dado o fileId. Nunca retorna null — sempre lança exceção em caso
   * de falha.
   */
  public File baixarArquivo(String fileId) {
    try {
      log.debug("Baixando arquivo do Telegram fileId={}", fileId);

      // Usa o método utilitário do facade
      org.telegram.telegrambots.meta.api.objects.File tgFile =
          telegramFacade.getFile(new GetFile(fileId));

      File localFile = telegramFacade.downloadFile(tgFile);

      if (localFile == null || !localFile.exists()) {
        throw new TelegramFileException("Arquivo não encontrado após download", null);
      }

      return localFile;

    } catch (TelegramApiException e) {
      log.error("Erro ao baixar arquivo fileId={}", fileId, e);
      throw new TelegramFileException("Falha ao baixar arquivo do Telegram", e);
    }
  }
}
