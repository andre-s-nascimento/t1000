/* (c) 2026 */
package net.ddns.adambravo79.tmill.service;

import java.io.File;
import java.util.function.BiConsumer;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.client.GroqClient;

/**
 * Orquestrador que une a conversão de áudio local com a inteligência na nuvem.
 *
 * <p>ATUALIZADO: Agora usa BiConsumer<String, Boolean> como callback: - O primeiro parâmetro é o
 * texto da mensagem a enviar. - O segundo (isUltimaMensagem) indica se é a mensagem final da
 * transcrição, momento em que o TelegramController deve anexar os botões de ação.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioPipelineService {

  private final AudioService audioService;
  private final GroqClient groqClient;
  private final TranscricaoCache transcricaoCache;

  /**
   * @param ogaFile Arquivo de áudio original baixado do Telegram
   * @param chatId ID do chat, necessário para salvar no cache
   * @param callback BiConsumer<textoMensagem, isUltimaMensagem>
   */
  public void processarFluxoAudio(File ogaFile, long chatId, BiConsumer<String, Boolean> callback) {
    log.info("Iniciando fluxo de processamento para: {}", ogaFile.getName());

    audioService
        .converterParaWav(ogaFile)
        .thenAccept(
            wavFile -> {
              // Passo 1: Transcrever e enviar o bruto imediatamente
              String bruto = groqClient.transcrever(wavFile);
              callback.accept("🎙️ *Bruto:* \n_" + bruto + "_", false);

              // Passo 2: Refinar
              String refinado = groqClient.refinarTexto(bruto);

              // Salva o texto refinado no cache para uso futuro pelo botão
              transcricaoCache.salvar(chatId, refinado);

              // Envia o refinado e sinaliza que é a última mensagem (true)
              // O controller vai anexar os botões nessa mensagem
              callback.accept("✨ *Refinado:* \n" + refinado, true);

              // Cleanup do arquivo temporário WAV
              wavFile.delete();
            })
        .exceptionally(
            ex -> {
              log.error("Falha no pipeline de áudio", ex);
              callback.accept("⚠️ Falha no processamento do áudio.", false);
              return null;
            })
        .thenRun(
            () -> {
              // Cleanup do arquivo original OGA
              ogaFile.delete();
              log.debug("Limpeza de arquivos temporários concluída.");
            });
  }
}
