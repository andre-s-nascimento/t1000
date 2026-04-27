/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * DTO que representa a resposta de um modelo de chat (Groq/Llama).
 *
 * <p>Campos: - choices: lista de opções retornadas pelo modelo, cada uma contendo uma mensagem.
 *
 * <p>Usado para mapear a resposta da API de chat completions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(List<Choice> choices) {}
