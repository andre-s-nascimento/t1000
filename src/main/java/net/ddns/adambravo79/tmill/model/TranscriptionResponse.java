package net.ddns.adambravo79.tmill.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Representa a resposta simples de transcrição do Whisper.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TranscriptionResponse(String text) {
}
