package net.ddns.adambravo79.tmill.service;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.ddns.adambravo79.tmill.client.TmdbClient;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private TmdbClient tmdbClient;

    @InjectMocks
    private MovieService movieService;

    @Test
    void deveRetornarMensagemQuandoFilmeNaoEncontrado() {
        when(tmdbClient.pesquisarFilme("xyz"))
                .thenReturn(null);

        var response = movieService.executarBuscaFormatada("xyz");

        assertThat(response.textoFormatado())
                .contains("Filme não encontrado");
    }

    @Test
    void deveFormatarRespostaCompleta() {
        Long id = 1L;

        when(tmdbClient.pesquisarFilme("duna"))
                .thenReturn(new MovieSearchResponse(1, 1, 1,
                        List.of(new MovieRecord(id, "Duna", "2021-01-01", "desc", 10.0, 8.5, "/img", List.of("US")))));

        when(tmdbClient.buscarDetalhes(id))
                .thenReturn(new MovieRecord(id, "Duna", "2021-01-01", "desc", 10.0, 8.5, "/img", List.of("US")));

        when(tmdbClient.buscarElenco(id))
                .thenReturn(List.of(new CastRecord("Timothée", "Paul")));

        when(tmdbClient.buscarOndeAssistir(id))
                .thenReturn("Netflix");

        var result = movieService.executarBuscaFormatada("duna");

        assertThat(result.textoFormatado())
                .contains("DUNA")
                .contains("2021")
                .contains("Netflix")
                .contains("Timothée");

        assertThat(result.urlFoto())
                .contains("image.tmdb.org");
    }
}
