/* (c) 2026 */
package net.ddns.adambravo79.tmill.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MovieRecord(
    @JsonProperty("id") Long id, // Precisamos do ID para buscar detalhes extras
    String title,
    @JsonProperty("release_date") String releaseDate,
    String overview,
    Double popularity, // <--- ADICIONE ESTA LINHA
    @JsonProperty("vote_average") Double voteAverage,
    @JsonProperty("poster_path") String posterPath,
    @JsonProperty("origin_country") List<String> originCountry // Lista de países (ex: ["US"])
    ) {}
