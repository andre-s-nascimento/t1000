package net.ddns.adambravo79.tmill.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.ddns.adambravo79.tmill.client.GroqClient;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Orquestrador que une a conversão de áudio local com a inteligência na nuvem.
 */
@Log4j2
@Service
@RequiredArgsConstructor // Lombok gera o construtor para injeção automática
public class AudioPipelineService {

    private final AudioService audioService; // Conversão local FFmpeg [cite: 4]
    private final GroqClient groqClient; // API Groq [cite: 41]

    public void processarFluxoAudio(File ogaFile, java.util.function.Consumer<String> callback) {
        log.info("Iniciando fluxo de processamento para: {}", ogaFile.getName());

        // Inicia a conversão assíncrona para WAV [cite: 3]
        audioService.converterParaWav(ogaFile)
                .thenAccept(wavFile -> {
                    // Passo 1: Transcrever
                    String bruto = groqClient.transcrever(wavFile);
                    callback.accept("🎙️ *Bruto:* \n_" + bruto + "_");

                    // Passo 2: Refinar
                    String refinado = groqClient.refinarTexto(bruto);
                    callback.accept("✨ *Refinado:* \n" + refinado);

                    // Cleanup do arquivo temporário WAV
                    wavFile.delete();
                })
                .exceptionally(ex -> {
                    log.error("Falha no pipeline de áudio", ex);

                    return null;
                })
                .thenRun(() -> {
                    // Cleanup do arquivo original OGA
                    ogaFile.delete();
                    log.debug("Limpeza de arquivos temporários concluída.");
                });
    }
}