package net.ddns.adambravo79.tmill.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MovieSearchResponse(
        int page,
        @JsonProperty("total_results") int totalResults,
        @JsonProperty("total_pages") int totalPages,
        List<MovieRecord> results // Aqui é onde os seus MovieRecords (com popularity) moram
) {
}