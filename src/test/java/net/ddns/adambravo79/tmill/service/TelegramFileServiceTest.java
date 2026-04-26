/* (c) 2026 */
package net.ddns.adambravo79.tmill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import net.ddns.adambravo79.tmill.telegram.TelegramFileException;

class TelegramFileServiceTest {

  @Test
  void deveBaixarArquivoComSucesso() throws Exception {
    TelegramClient client = mock(TelegramClient.class);
    TelegramFileService service = new TelegramFileService(client);

    // cria arquivo temporário
    File temp = Files.createTempFile("audio", ".tmp").toFile();

    // usa nome totalmente qualificado para o File do Telegram
    org.telegram.telegrambots.meta.api.objects.File tgFile =
        new org.telegram.telegrambots.meta.api.objects.File();

    when(client.execute(any(GetFile.class))).thenReturn(tgFile);
    when(client.downloadFile(tgFile)).thenReturn(temp);

    File result = service.baixarArquivo("file-id");

    assertThat(result).exists();
    assertThat(result.getName()).endsWith(".oga");
    assertThat(result.getParentFile()).hasName("temp_audio");
  }

  @Test
  void deveLancarExcecaoQuandoFalhar() throws Exception {
    TelegramClient client = mock(TelegramClient.class);
    TelegramFileService service = new TelegramFileService(client);

    when(client.execute(any(GetFile.class))).thenThrow(new RuntimeException("erro"));

    assertThatThrownBy(() -> service.baixarArquivo("file-id"))
        .isInstanceOf(TelegramFileException.class)
        .hasMessageContaining("Erro ao baixar arquivo do Telegram");
  }
}
