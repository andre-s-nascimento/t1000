/* (c) 2026 */
package net.ddns.adambravo79.tmill.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Provider(
    @JsonProperty("provider_name") String name,
    @JsonProperty("provider_id") Integer id,
    @JsonProperty("logo_path") String logoPath) {}
