package net.ddns.adambravo79.tmill.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Message precisa ser public para ser usada no record public principal.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Message(
        String role,
        String content) {
}