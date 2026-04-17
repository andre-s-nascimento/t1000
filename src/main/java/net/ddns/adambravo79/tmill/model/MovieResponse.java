package net.ddns.adambravo79.tmill.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MovieResponse(
                @JsonProperty("results") List<MovieRecord> results) {
}
