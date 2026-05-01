/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO que representa a resposta de créditos de um filme no TMDB.
 *
 * <p>Campos: - cast: lista de membros do elenco ({@link CastRecord}).
 *
 * <p>Usado para mapear o endpoint `/movie/{id}/credits` da API do TMDB.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreditsResponse(@JsonProperty("cast") List<CastRecord> cast) {}
