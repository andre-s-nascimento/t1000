package net.ddns.adambravo79.tmill.client;

import lombok.extern.log4j.Log4j2;
import net.ddns.adambravo79.tmill.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

/**
 * Cliente de baixo nível para integração com a API v3 do TMDB.
 * Isolado para facilitar a manutenção de URLs e Headers.
 */
@Log4j2
@Component
public class TmdbClient {

    // Nosso Mapa de Atalhos (A "Gambi" de Ouro)
    private static final Map<String, String> ATALHOS = Map.of(
            "duna", "Dune 2021",
            "dune", "Dune 2021",
            "batman", "The Batman 2022",
            "o poderoso chefao", "The Godfather 1972");

    private final RestClient restClient;

    public TmdbClient(@Value("${tmdb.token}") String tmdbToken,
            @Value("${tmdb.api.url}") String apiUrl) {
        // Inicializa o RestClient com autenticação via Bearer Token e headers padrão
        // [cite: 22, 23]
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + tmdbToken)
                .defaultHeader("accept", "application/json")
                .build();
    }

    /**
     * UNIFICADO: Agora este é o único método de busca.
     * Ele aplica atalhos e retorna a resposta completa com a lista de filmes.
     */
    public MovieSearchResponse pesquisarFilme(String query) {
        String queryNormalizada = query.trim().toLowerCase();
        String queryFinal = ATALHOS.getOrDefault(queryNormalizada, query);

        if (!queryFinal.equals(query)) {
            log.info("TMDB: Atalho de Mestre aplicado! '{}' -> '{}'", query, queryFinal);
        }

        log.info("TMDB: Pesquisando -> {}", queryFinal);
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/movie")
                            .queryParam("query", queryFinal)
                            .queryParam("language", "pt-BR")
                            .queryParam("region", "BR")
                            .queryParam("include_adult", "false")
                            .build())
                    .retrieve()
                    .body(MovieSearchResponse.class);
        } catch (Exception e) {
            log.error("TMDB: Falha na busca por '{}': {}", queryFinal, e.getMessage());
            return null;
        }
    }

    /**
     * Busca básica de filmes por nome. [cite: 24]
     */
    // public MovieResponse buscarFilme(String nomeFilme) {
    // log.info("TMDB: Pesquisando filme -> {}", nomeFilme);
    // try {
    // return restClient.get()
    // .uri(uriBuilder -> uriBuilder
    // .path("/search/movie")
    // .queryParam("query", nomeFilme)
    // .queryParam("language", "pt-BR")
    // .queryParam("region", "BR") // Foco no mercado brasileiro [cite: 25]
    // .queryParam("include_adult", "false") // Filtro de segurança [cite: 26]
    // .build())
    // .retrieve()
    // .body(MovieResponse.class);
    // } catch (Exception e) {
    // log.error("Erro na busca do TMDB", e);
    // return null;
    // }
    // }

    /**
     * Obtém detalhes técnicos de um filme específico pelo ID. [cite: 34]
     */
    public MovieRecord buscarDetalhes(Long movieId) {
        log.debug("TMDB: Buscando detalhes do ID {}", movieId);
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam("language", "pt-BR")
                        .build(movieId))
                .retrieve()
                .body(MovieRecord.class); // Preenche o record com dados completos [cite: 35, 36]
    }

    /**
     * Busca a lista de elenco (Cast). [cite: 36]
     */
    public List<CastRecord> buscarElenco(Long movieId) {
        log.debug("TMDB: Buscando elenco do ID {}", movieId);
        try {
            CreditsResponse response = restClient.get()
                    .uri("/movie/{id}/credits", movieId)
                    .retrieve()
                    .body(CreditsResponse.class);
            return (response != null) ? response.cast() : List.of();
        } catch (Exception e) {
            log.warn("Falha ao obter elenco: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Identifica provedores de streaming (Flatrate) disponíveis no Brasil. [cite:
     * 28]
     */
    public String buscarOndeAssistir(Long movieId) {
        log.debug("TMDB: Verificando provedores para o ID {}", movieId);
        try {
            WatchProviderResponse response = restClient.get()
                    .uri("/movie/{id}/watch/providers", movieId)
                    .retrieve()
                    .body(WatchProviderResponse.class);

            // Filtra especificamente para o mercado brasileiro (BR) [cite: 29]
            if (response != null && response.results().containsKey("BR")) {
                var brProviders = response.results().get("BR").flatrate();
                if (brProviders != null && !brProviders.isEmpty()) {
                    return brProviders.stream()
                            .map(p -> p.name()) // Mapeia para o nome do streaming [cite: 30]
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("Não disponível em streaming.");
                }
            }
            return "Disponível apenas para Aluguel/Compra.";
        } catch (Exception e) {
            log.error("Erro ao buscar provedores", e);
            return "Informação indisponível.";
        }
    }
}