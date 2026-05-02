/* (c) 2026 | 02/05/2026 */
package net.ddns.adambravo79.tmill.client;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.model.ChatCompletionResponse;
import net.ddns.adambravo79.tmill.model.TranscriptionResponse;

@Slf4j
@Component
public class GroqClient {

    private final RestClient restClient;
    private final int maxRefinementLength;

    @Autowired
    public GroqClient(
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.max-refinement-length:10000}") int maxRefinementLength) {
        this.restClient =
                RestClient.builder()
                        .baseUrl("https://api.groq.com")
                        .defaultHeader("Authorization", "Bearer " + apiKey)
                        .build();
        this.maxRefinementLength = maxRefinementLength;
    }

    // Construtor para testes
    public GroqClient(RestClient restClient, int maxRefinementLength) {
        this.restClient = restClient;
        this.maxRefinementLength = maxRefinementLength;
    }

    public String transcrever(File wavFile) {
        log.info("🎙️ Iniciando transcrição via Groq (Whisper) para arquivo={}", wavFile.getName());

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new org.springframework.core.io.FileSystemResource(wavFile));
        builder.part("model", "whisper-large-v3");

        TranscriptionResponse response =
                restClient
                        .post()
                        .uri("/openai/v1/audio/transcriptions")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(builder.build())
                        .retrieve()
                        .body(TranscriptionResponse.class);

        if (response == null || response.text() == null) {
            log.error("❌ Groq: resposta inválida na transcrição arquivo={}", wavFile.getName());
            throw new IllegalStateException("Falha na transcrição — resposta inválida");
        }

        log.info(
                "✅ Transcrição concluída arquivo={} textoSize={}",
                wavFile.getName(),
                response.text().length());
        return response.text();
    }

    public String refinarTexto(String textoBruto) {
        // Se ultrapassar o limite configurável, retorna o bruto com aviso (não lança exceção)
        if (textoBruto.length() > maxRefinementLength) {
            log.warn(
                    "⚠️ Texto muito longo para refinamento automático size={} (limite={})",
                    textoBruto.length(),
                    maxRefinementLength);
            return "⚠️ *Aviso:* O texto excede o limite para refinamento (máx. "
                    + maxRefinementLength
                    + " caracteres). Segue a versão bruta:\n\n"
                    + textoBruto;
        }

        log.debug("✨ Refinando texto via Llama 3.1 size={}", textoBruto.length());

        var payload =
                Map.of(
                        "model",
                        "llama-3.1-8b-instant",
                        "messages",
                        List.of(
                                Map.of(
                                        "role",
                                        "system",
                                        "content",
                                        "Corrija a pontuação e remova vícios de fala. Retorne"
                                                + " apenas o texto limpo."),
                                Map.of("role", "user", "content", textoBruto)),
                        "temperature",
                        0.3);

        ChatCompletionResponse response =
                restClient
                        .post()
                        .uri("/openai/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(ChatCompletionResponse.class);

        if (response == null || response.choices().isEmpty()) {
            log.error("❌ Groq: resposta inválida no refinamento textoSize={}", textoBruto.length());
            // Fallback: retorna o texto bruto
            return textoBruto;
        }

        String textoRefinado = response.choices().get(0).message().content();
        log.info(
                "✅ Refinamento concluído textoSize={} refinadoSize={}",
                textoBruto.length(),
                textoRefinado.length());
        return textoRefinado;
    }
}
