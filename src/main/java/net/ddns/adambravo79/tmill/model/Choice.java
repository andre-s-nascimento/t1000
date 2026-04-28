/* (c) 2026-2026 */
package net.ddns.adambravo79.tmill.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Choice precisa ser public para ser usada no record public principal. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Choice(Message message) {}
