package net.ddns.adambravo79.tmill.service;

import lombok.extern.log4j.Log4j2;
import net.ddns.adambravo79.tmill.client.TmdbClient;
import net.ddns.adambravo79.tmill.model.MovieOrchestrationResponse;
import net.ddns.adambravo79.tmill.model.MovieSearchResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
public class MovieService {

    private final TmdbClient tmdbClient;

    public MovieService(TmdbClient tmdbClient) {
        this.tmdbClient = tmdbClient;
    }

    /**
     * Agora retorna o objeto de busca unificado.
     */
    public MovieSearchResponse buscarFilme(String nome) {
        return tmdbClient.pesquisarFilme(nome);
    }

    /**
     * Executa a busca formatada aplicando a lógica de desambiguação automática.
     */
    public MovieOrchestrationResponse executarBuscaFormatada(String nome) {
        // Chamando o método unificado que já aplica os ATALHOS (Gambi do Duna)
        var busca = tmdbClient.pesquisarFilme(nome);

        if (busca == null || busca.results() == null || busca.results().isEmpty()) {
            return new MovieOrchestrationResponse("❌ Filme não encontrado.", null);
        }

        // Se o usuário digitou exatamente o nome (ou o atalho injetou o nome exato)
        // pegamos o primeiro resultado da lista.
        var basico = busca.results().get(0);

        return buscarPorId(basico.id());
    }

    /**
     * Busca detalhes diretamente pelo ID do TMDB.
     */
    public MovieOrchestrationResponse buscarPorId(long id) {
        var detalhes = tmdbClient.buscarDetalhes(id);
        if (detalhes == null) {
            return new MovieOrchestrationResponse("❌ Detalhes do filme não encontrados.", null);
        }

        var elenco = tmdbClient.buscarElenco(id).stream()
                .limit(5)
                .map(c -> c.name())
                .collect(Collectors.joining(", "));

        var streamings = tmdbClient.buscarOndeAssistir(id);

        String ano = (detalhes.releaseDate() != null && detalhes.releaseDate().length() >= 4)
                ? detalhes.releaseDate().substring(0, 4)
                : "TBA";

        String bandeiras = "🌐";
        if (detalhes.originCountry() != null && !detalhes.originCountry().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String code : detalhes.originCountry()) {
                getFlagEmoji(code).ifPresent(sb::append);
            }
            if (sb.length() > 0)
                bandeiras = sb.toString();
        }

        String linkTmdb = "https://www.themoviedb.org/movie/" + detalhes.id();

        String texto = String.format(
                "🎬 *%s*\n" +
                        "📅 Ano: %s %s\n" +
                        "⭐ *Nota:* [%.1f/10](%s)\n\n" +
                        "📺 *Onde assistir:* %s\n\n" +
                        "👥 *Elenco:* %s\n\n" +
                        "📖 *Sinopse:* %s",
                detalhes.title().toUpperCase(),
                ano,
                bandeiras,
                detalhes.voteAverage(),
                linkTmdb,
                streamings,
                elenco,
                detalhes.overview());

        String urlPoster = "https://image.tmdb.org/t/p/w500" + detalhes.posterPath();

        return new MovieOrchestrationResponse(texto, urlPoster);
    }

    private Optional<String> getFlagEmoji(String countryCode) {
        if (countryCode == null || countryCode.length() != 2)
            return Optional.empty();
        int firstLetter = Character.codePointAt(countryCode.toUpperCase(), 0) - 0x41 + 0x1F1E6;
        int secondLetter = Character.codePointAt(countryCode.toUpperCase(), 1) - 0x41 + 0x1F1E6;
        return Optional.of(new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter)));
    }
}