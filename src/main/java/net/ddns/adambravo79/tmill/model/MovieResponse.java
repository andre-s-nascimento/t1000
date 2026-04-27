/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO que representa a resposta de busca de filmes na API do TMDB.
 *
 * <p>Campos: - results: lista de filmes encontrados ({@link MovieRecord}).
 *
 * <p>Usado para mapear o endpoint `/search/movie` da API do TMDB.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MovieResponse(@JsonProperty("results") List<MovieRecord> results) {}
