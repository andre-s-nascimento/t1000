package net.ddns.adambravo79.tmill.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Nova estrutura para o elenco
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreditsResponse(
        @JsonProperty("cast") List<CastRecord> cast) {
}
