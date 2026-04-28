/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MovieSearchResponse(
        int page,
        @JsonProperty("total_results") int totalResults,
        @JsonProperty("total_pages") int totalPages,
        List<MovieRecord> results // Aqui é onde os seus MovieRecords (com popularity) moram
        ) {}
