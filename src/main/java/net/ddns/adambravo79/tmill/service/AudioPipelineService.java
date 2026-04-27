/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.cache.TranscricaoCache;
import net.ddns.adambravo79.tmill.client.GroqClient;
import net.ddns.adambravo79.tmill.exception.AudioProcessingException;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por orquestrar o pipeline de processamento de áudio.
 *
 * <p>Etapas do fluxo: 1. Converte o arquivo OGA para WAV. 2. Transcreve o áudio bruto via {@link
 * GroqClient}. 3. Refina o texto transcrito. 4. Armazena a transcrição refinada em {@link
 * TranscricaoCache}. 5. Retorna os resultados via callback.
 *
 * <p>Em caso de falha, lança {@link AudioProcessingException} com contexto detalhado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioPipelineService {

  private final AudioService audioService;
  private final GroqClient groqClient;
  private final TranscricaoCache transcricaoCache;

  /**
   * Processa o fluxo completo de áudio, desde a conversão até a transcrição refinada.
   *
   * @param ogaFile arquivo de áudio recebido (formato OGA).
   * @param chatId identificador do chat para cache da transcrição.
   * @param callback função de retorno que recebe o texto transcrito e um indicador de refinamento.
   * @throws AudioProcessingException em caso de falha em qualquer etapa do pipeline.
   */
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
                  throw new CompletionException(e);
                } catch (Exception e) {
                  throw new CompletionException(
                      new AudioProcessingException(
                          "Falha no pipeline de áudio para arquivo: " + wavFile.getName(), e));
                } finally {
                  deletarSilenciosamente(wavFile);
                }
              })
          .exceptionally(
              ex -> {
                Throwable causa =
                    (ex instanceof CompletionException && ex.getCause() != null)
                        ? ex.getCause()
                        : ex;

                if (causa instanceof AudioProcessingException) {
                  throw new CompletionException(causa);
                }

                throw new CompletionException(
                    new AudioProcessingException(
                        "Erro inesperado no pipeline de áudio para arquivo:"
                            + " "
                            + ogaFile.getName(),
                        causa));
              })
          .thenRun(() -> deletarSilenciosamente(ogaFile))
          .join();

    } catch (CompletionException e) {
      Throwable causa = e.getCause() != null ? e.getCause() : e;
      if (causa instanceof AudioProcessingException ape) {
        throw ape;
      }
      throw new AudioProcessingException(
          "Erro inesperado no pipeline de áudio para arquivo: " + ogaFile.getName(), causa);
    }
  }

  /**
   * Exclui um arquivo temporário silenciosamente, sem interromper o fluxo em caso de falha.
   *
   * @param file arquivo a ser excluído.
   */
  private void deletarSilenciosamente(File file) {
    try {
      Files.delete(Path.of(file.getAbsolutePath()));
      log.debug("Arquivo temporário excluído: {}", file.getAbsolutePath());
    } catch (IOException ex) {
      log.warn("Não foi possível excluir arquivo temporário: {}", file.getAbsolutePath(), ex);
    }
  }
}
