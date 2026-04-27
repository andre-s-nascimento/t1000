/* (c) 2026 */
package net.ddns.adambravo79.tmill.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.client.GroqClient;
import net.ddns.adambravo79.tmill.exception.AudioProcessingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioPipelineService {

  private final AudioService audioService;
  private final GroqClient groqClient;
  private final TranscricaoCache transcricaoCache;

  public void processarFluxoAudio(File ogaFile, long chatId, BiConsumer<String, Boolean> callback) {
    log.info("Iniciando fluxo de processamento para: {}", ogaFile.getName());

    try {
      audioService
          .converterParaWav(ogaFile)
          .thenAccept(
              wavFile -> {
                try {
                  String bruto = groqClient.transcrever(wavFile);
                  callback.accept("🎙️ *Bruto:* \n_" + bruto + "_", false);

                  String refinado = groqClient.refinarTexto(bruto);
                  transcricaoCache.salvar(chatId, refinado);
                  callback.accept("✨ *Refinado:* \n" + refinado, true);

                } catch (AudioProcessingException e) {
                  // já é do tipo certo — relança como CompletionException para o join()
                  throw new CompletionException(e);
                } catch (Exception e) {
                  // embrulha qualquer outra exceção
                  throw new CompletionException(
                      new AudioProcessingException(
                          "Falha no pipeline de áudio para arquivo: " + wavFile.getName(), e));
                } finally {
                  deletarSilenciosamente(wavFile);
                }
              })
          .exceptionally(
              ex -> {
                // CompletableFuture.failedFuture() chega aqui com a causa direto
                Throwable causa =
                    (ex instanceof CompletionException && ex.getCause() != null)
                        ? ex.getCause()
                        : ex;

                if (causa instanceof AudioProcessingException) {
                  throw new CompletionException(causa);
                }

                throw new CompletionException(
                    new AudioProcessingException(
                        "Erro inesperado no pipeline de áudio para arquivo: " + ogaFile.getName(),
                        causa));
              })
          .thenRun(() -> deletarSilenciosamente(ogaFile))
          .join(); // propaga CompletionException para cá

    } catch (CompletionException e) {
      // ✅ Desembrulha: join() sempre embrulha em CompletionException
      Throwable causa = e.getCause() != null ? e.getCause() : e;
      if (causa instanceof AudioProcessingException ape) {
        throw ape;
      }
      throw new AudioProcessingException(
          "Erro inesperado no pipeline de áudio para arquivo: " + ogaFile.getName(), causa);
    }
  }

  private void deletarSilenciosamente(File file) {
    try {
      Files.delete(Path.of(file.getAbsolutePath()));
      log.debug("Arquivo temporário excluído: {}", file.getAbsolutePath());
    } catch (IOException ex) {
      log.warn("Não foi possível excluir arquivo temporário: {}", file.getAbsolutePath(), ex);
    }
  }
}
