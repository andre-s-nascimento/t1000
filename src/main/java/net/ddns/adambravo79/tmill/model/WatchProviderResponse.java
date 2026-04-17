package net.ddns.adambravo79.tmill.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

// Estrutura para /movie/{id}/watch/providers
@JsonIgnoreProperties(ignoreUnknown = true)
public record WatchProviderResponse(
                @JsonProperty("results") Map<String, CountryProviders> results) {
}
