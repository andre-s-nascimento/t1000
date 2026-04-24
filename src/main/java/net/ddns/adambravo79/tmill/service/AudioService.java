package net.ddns.adambravo79.tmill.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service técnico para manipulação de arquivos de áudio via FFmpeg.
 * Otimizado para rodar dentro do container Docker na OCI.
 */
@Log4j2
@Service
public class AudioService {

    /**
     * Converte OGA (Opus) para WAV (16kHz, Mono) para garantir
     * compatibilidade máxima com o modelo Whisper no Groq.
     */
    @Async
    public CompletableFuture<File> converterParaWav(File ogaFile) {
        File wavFile = new File(ogaFile.getAbsolutePath().replace(".oga", ".wav"));
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", ogaFile.getAbsolutePath(),
                    "-ar", "16000", "-ac", "1", wavFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            log.info("FFmpeg: Iniciando conversão de {}...", ogaFile.getName());

            Process p = startProcess(pb); // ✅ chamada ao método protegido

            boolean finished = p.waitFor(30, TimeUnit.SECONDS);
            if (finished && p.exitValue() == 0) {
                log.info("FFmpeg: Conversão concluída com sucesso.");
                return CompletableFuture.completedFuture(wavFile);
            } else {
                log.error("FFmpeg: Falha na conversão ou timeout atingido.");
                return CompletableFuture.failedFuture(new RuntimeException("FFmpeg falhou"));
            }
        } catch (Exception e) {
            log.error("Erro crítico no AudioService", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    // 👇 ADICIONE ESTE MÉTODO
    protected Process startProcess(ProcessBuilder pb) throws Exception {
        return pb.start();
    }
}