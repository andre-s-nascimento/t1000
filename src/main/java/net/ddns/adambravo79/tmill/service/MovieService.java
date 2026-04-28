/* (c) 2026-2026 */
package net.ddns.adambravo79.tmill.service;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.client.TmdbClient;
import net.ddns.adambravo79.tmill.exception.MovieNotFoundException;
import net.ddns.adambravo79.tmill.model.MovieOrchestrationResponse;
import net.ddns.adambravo79.tmill.model.MovieSearchResponse;

@Slf4j
@Service
public class MovieService {

    private final TmdbClient tmdbClient;

    public MovieService(TmdbClient tmdbClient) {
        this.tmdbClient = tmdbClient;
    }

    /** Agora retorna o objeto de busca unificado. */
    public MovieSearchResponse buscarFilme(String nome) {
        var busca = tmdbClient.pesquisarFilme(nome);
        if (busca == null || busca.results() == null || busca.results().isEmpty()) {
            throw new MovieNotFoundException("Filme não encontrado: " + nome);
        }
        return busca;
    }

    /** Executa a busca formatada aplicando a lógica de desambiguação automática. */
    public MovieOrchestrationResponse executarBuscaFormatada(String nome) {
        var busca = buscarFilme(nome); // ✅ já lança exceção se não encontrar

        var basico = busca.results().get(0);
        return buscarPorId(basico.id());
    }

    /** Busca detalhes diretamente pelo ID do TMDB. */
    public MovieOrchestrationResponse buscarPorId(long id) {
        var detalhes = tmdbClient.buscarDetalhes(id);
        if (detalhes == null) {
            throw new MovieNotFoundException("Detalhes do filme não encontrados para ID: " + id);
        }

        var elenco =
                tmdbClient.buscarElenco(id).stream()
                        .limit(5)
                        .map(c -> c.name())
                        .collect(Collectors.joining(", "));

        var streamings = tmdbClient.buscarOndeAssistir(id);

        String ano =
                (detalhes.releaseDate() != null && detalhes.releaseDate().length() >= 4)
                        ? detalhes.releaseDate().substring(0, 4)
                        : "TBA";

        String bandeiras = "🌐";
        if (detalhes.originCountry() != null && !detalhes.originCountry().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String code : detalhes.originCountry()) {
                getFlagEmoji(code).ifPresent(sb::append);
            }
            if (!sb.isEmpty()) bandeiras = sb.toString();
        }

        String linkTmdb = "https://www.themoviedb.org/movie/" + detalhes.id();

        String texto =
                String.format(
                        """
            🎬 *%s*
            📅 Ano: %s %s
            ⭐ *Nota:* [%.1f/10](%s)

            📺 *Onde assistir:* %s

            👥 *Elenco:* %s

            📖 *Sinopse:* %s
            """,
                        detalhes.title().toUpperCase(),
                        ano,
                        bandeiras,
                        detalhes.voteAverage(),
                        linkTmdb,
                        streamings,
                        elenco,
                        escapeMarkdown(detalhes.overview()));

        String urlPoster = "https://image.tmdb.org/t/p/w500" + detalhes.posterPath();

        return new MovieOrchestrationResponse(texto, urlPoster);
    }

    private Optional<String> getFlagEmoji(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) return Optional.empty();
        int firstLetter = Character.codePointAt(countryCode.toUpperCase(), 0) - 0x41 + 0x1F1E6;
        int secondLetter = Character.codePointAt(countryCode.toUpperCase(), 1) - 0x41 + 0x1F1E6;
        return Optional.of(
                new String(Character.toChars(firstLetter))
                        + new String(Character.toChars(secondLetter)));
    }

    private String escapeMarkdown(String texto) {
        return texto.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
}
