/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.dto;

import java.time.Instant;

public record ErrorResponse(String erro, String tipo, Instant timestamp) {}
