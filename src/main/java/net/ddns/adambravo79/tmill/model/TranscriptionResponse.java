/* (c) 2026 */
package net.ddns.adambravo79.tmill.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Representa a resposta simples de transcrição do Whisper. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TranscriptionResponse(String text) {}
