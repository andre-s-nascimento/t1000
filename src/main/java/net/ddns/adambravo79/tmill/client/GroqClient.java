package net.ddns.adambravo79.tmill.client;

import lombok.extern.log4j.Log4j2; // Lombok para logging
import net.ddns.adambravo79.tmill.model.TranscriptionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Cliente responsável pela comunicação com a API do Groq (IA).
 * Utiliza RestClient do Spring para chamadas síncronas/assíncronas.
 */
@Log4j2
@Component
public class GroqClient {

    private final RestClient restClient;

    public GroqClient(@Value("${groq.api.key}") String apiKey) {
        // Inicializa o cliente com a URL base do Groq e header de autorização
        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * Envia um arquivo WAV para transcrição usando o modelo Whisper.
     */
    public String transcrever(File wavFile) {
        log.info("Iniciando transcrição via Groq (Whisper) para o arquivo: {}", wavFile.getName());
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            // Adiciona o arquivo como recurso do sistema de arquivos
            builder.part("file", new org.springframework.core.io.FileSystemResource(wavFile));
            builder.part("model", "whisper-large-v3");

            TranscriptionResponse response = restClient.post()
                    .uri("/openai/v1/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(TranscriptionResponse.class);

            return (response != null) ? response.text() : "Falha na transcrição.";
        } catch (Exception e) {
            log.error("Erro na transcrição Groq", e);
            return "⚠️ Erro na unidade de voz.";
        }
    }

    /**
     * Envia texto para o Llama 3.1 para correção e refinamento gramatical.
     */
    public String refinarTexto(String textoBruto) {
        // Proteção contra textos excessivamente longos (economiza tokens)
        if (textoBruto.length() > 5000)
            return textoBruto;

        log.debug("Refinando texto via Llama 3.1");
        try {
            var payload = Map.of(
                    "model", "llama-3.1-8b-instant", // Modelo atualizado e ativo [cite: 45]
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "Corrija a pontuação e remova vícios de fala. Retorne apenas o texto limpo."),
                            Map.of("role", "user", "content", textoBruto)),
                    "temperature", 0.3 // Baixa temperatura para evitar alucinações
            );

            var response = restClient.post()
                    .uri("/openai/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            // Navega no JSON de resposta para extrair o conteúdo da mensagem
            var choices = (List<Map<String, Object>>) response.get("choices");
            var message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("Erro no refinamento Llama", e);
            return textoBruto; // Se falhar o refinamento, retorna ao menos o bruto
        }
    }
}