/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Estrutura para /movie/{id}/watch/providers
@JsonIgnoreProperties(ignoreUnknown = true)
public record WatchProviderResponse(
        @JsonProperty("results") Map<String, CountryProviders> results) {}
