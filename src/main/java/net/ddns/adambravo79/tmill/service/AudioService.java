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
        // Gera o nome do arquivo .wav na mesma pasta temporária
        File wavFile = new File(ogaFile.getAbsolutePath().replace(".oga", ".wav"));

        try {
            // Comando FFmpeg configurado para o padrão que o Groq/OpenAI prefere (16000Hz,
            // Mono)
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", ogaFile.getAbsolutePath(),
                    "-ar", "16000",
                    "-ac", "1",
                    wavFile.getAbsolutePath());

            // Redireciona erros para o log do Spring para facilitar debug na OCI
            pb.redirectErrorStream(true);

            log.info("FFmpeg: Iniciando conversão de {}...", ogaFile.getName());

            Process p = pb.start();

            // Timeout de 30s para evitar processos zumbis na OCI
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
}