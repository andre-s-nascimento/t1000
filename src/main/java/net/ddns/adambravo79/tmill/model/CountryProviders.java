/* (c) 2026 */
package net.ddns.adambravo79.tmill.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CountryProviders(
    @JsonProperty("flatrate") List<Provider> flatrate // Apenas assinaturas (Netflix, Prime, etc)
    ) {}
