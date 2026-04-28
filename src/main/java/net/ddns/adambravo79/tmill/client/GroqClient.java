/* (c) 2026 | 27/04/2026 */
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

    // ✅ Construtor principal usado pelo Spring
    @Autowired
    public GroqClient(@Value("${groq.api.key}") String apiKey) {
        this.restClient =
                RestClient.builder()
                        .baseUrl("https://api.groq.com")
                        .defaultHeader("Authorization", "Bearer " + apiKey)
                        .build();
    }

    // ✅ Construtor alternativo para testes (sem apiKey, apenas RestClient mockado)
    public GroqClient(RestClient restClient) {
        this.restClient = restClient;
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
        if (textoBruto.length() > 5000) {
            log.warn(
                    "⚠️ Texto muito longo para refinamento automático size={}",
                    textoBruto.length());
            throw new IllegalArgumentException("Texto muito longo para refinamento automático");
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
            throw new IllegalStateException("Falha no refinamento — resposta inválida");
        }

        String textoRefinado = response.choices().get(0).message().content();
        log.info(
                "✅ Refinamento concluído textoSize={} refinadoSize={}",
                textoBruto.length(),
                textoRefinado.length());
        return textoRefinado;
    }
}
