/* (c) 2026 */
package net.ddns.adambravo79.tmill.service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.exception.AudioProcessingException;

/**
 * Service técnico para manipulação de arquivos de áudio via FFmpeg. Otimizado para rodar dentro do
 * container Docker na OCI.
 */
@Slf4j
@Service
public class AudioService {

  @Async
  public CompletableFuture<File> converterParaWav(File ogaFile) {
    File wavFile = new File(ogaFile.getAbsolutePath().replace(".oga", ".wav"));

    try {
      ProcessBuilder pb =
          new ProcessBuilder(
              "ffmpeg",
              "-y",
              "-i",
              ogaFile.getAbsolutePath(),
              "-ar",
              "16000",
              "-ac",
              "1",
              wavFile.getAbsolutePath());

      pb.redirectErrorStream(true);

      log.info("FFmpeg: Iniciando conversão de {}...", ogaFile.getName());

      Process p = startProcess(pb);

      boolean finished = p.waitFor(30, TimeUnit.SECONDS);

      if (finished && p.exitValue() == 0) {
        log.info("FFmpeg: Conversão concluída com sucesso.");
        return CompletableFuture.completedFuture(wavFile);
      }

      log.error("FFmpeg: Falha na conversão ou timeout.");
      return CompletableFuture.failedFuture(new AudioProcessingException("FFmpeg falhou"));

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // 🔥 obrigatório
      return CompletableFuture.failedFuture(
          new AudioProcessingException("Processo interrompido", e));

    } catch (Exception e) {
      log.error("Erro crítico no AudioService", e);
      return CompletableFuture.failedFuture(new AudioProcessingException("FFmpeg falhou", e));
    }
  }

  protected Process startProcess(ProcessBuilder pb) throws IOException {
    return pb.start();
  }
}
