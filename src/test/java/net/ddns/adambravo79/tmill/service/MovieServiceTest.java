/* (c) 2026 */
package net.ddns.adambravo79.tmill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.ddns.adambravo79.tmill.client.TmdbClient;
import net.ddns.adambravo79.tmill.exception.MovieNotFoundException;
import net.ddns.adambravo79.tmill.model.CastRecord;
import net.ddns.adambravo79.tmill.model.MovieRecord;
import net.ddns.adambravo79.tmill.model.MovieSearchResponse;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

  @Mock private TmdbClient tmdbClient;

  @InjectMocks private MovieService movieService;

  @Test
  void deveLancarExcecaoQuandoFilmeNaoEncontrado() {
    when(tmdbClient.pesquisarFilme("xyz")).thenReturn(null);

    assertThatThrownBy(() -> movieService.executarBuscaFormatada("xyz"))
        .isInstanceOf(MovieNotFoundException.class)
        .hasMessageContaining("Filme não encontrado");
  }

  @Test
  void deveFormatarRespostaCompleta() {
    Long id = 1L;

    when(tmdbClient.pesquisarFilme("agente secreto"))
        .thenReturn(
            new MovieSearchResponse(
                1,
                1,
                1,
                List.of(
                    new MovieRecord(
                        id,
                        "O Agente Secreto",
                        "2025-09-10",
                        "desc",
                        10.0,
                        8.5,
                        "/img",
                        List.of("BR")))));

    when(tmdbClient.buscarDetalhes(id))
        .thenReturn(
            new MovieRecord(
                id, "O Agente Secreto", "2026-01-01", "desc", 10.0, 8.5, "/img", List.of("BR")));

    when(tmdbClient.buscarElenco(id))
        .thenReturn(List.of(new CastRecord("Wagner Moura", "Marcelo")));

    when(tmdbClient.buscarOndeAssistir(id)).thenReturn("Netflix");

    var result = movieService.executarBuscaFormatada("agente secreto");

    assertThat(result.textoFormatado())
        .contains("O AGENTE SECRETO")
        .contains("2026")
        .contains("Netflix")
        .contains("Wagner")
        .matches("(?s).*🇧🇷.*");

    assertThat(result.urlFoto()).contains("image.tmdb.org");
  }

  @Test
  void deveLancarExcecaoQuandoDetalhesForemNull() {
    when(tmdbClient.buscarDetalhes(1L)).thenReturn(null);

    assertThatThrownBy(() -> movieService.buscarPorId(1L))
        .isInstanceOf(MovieNotFoundException.class)
        .hasMessageContaining("Detalhes do filme não encontrados");
  }

  @Test
  void deveUsarGloboQuandoNaoHouverPais() {
    var movie =
        new MovieRecord(1L, "O Agente Secreto", "2025-09-10", "desc", 10.0, 8.5, "/img", List.of());

    when(tmdbClient.buscarDetalhes(1L)).thenReturn(movie);
    when(tmdbClient.buscarElenco(1L)).thenReturn(List.of());
    when(tmdbClient.buscarOndeAssistir(1L)).thenReturn("N/A");

    var result = movieService.buscarPorId(1L);

    assertThat(result.textoFormatado()).contains("🌐");
  }

  @Test
  void deveLancarExcecaoQuandoListaVazia() {
    when(tmdbClient.pesquisarFilme("x")).thenReturn(new MovieSearchResponse(1, 1, 1, List.of()));

    assertThatThrownBy(() -> movieService.executarBuscaFormatada("x"))
        .isInstanceOf(MovieNotFoundException.class)
        .hasMessageContaining("Filme não encontrado");
  }

  @Test
  void deveUsarTBAQuandoSemData() {
    Long id = 1L;

    var movie = new MovieRecord(id, "Teste", null, "desc", 1.0, 1.0, "/img", List.of("US"));

    when(tmdbClient.buscarDetalhes(id)).thenReturn(movie);
    when(tmdbClient.buscarElenco(id)).thenReturn(List.of());
    when(tmdbClient.buscarOndeAssistir(id)).thenReturn("N/A");

    var result = movieService.buscarPorId(id);

    assertThat(result.textoFormatado()).contains("TBA");
  }

  @Test
  void deveUsarGloboQuandoPaisInvalido() {
    var movie = new MovieRecord(1L, "Teste", "2020", "desc", 1.0, 1.0, "/img", List.of("XXX"));

    when(tmdbClient.buscarDetalhes(1L)).thenReturn(movie);
    when(tmdbClient.buscarElenco(1L)).thenReturn(List.of());
    when(tmdbClient.buscarOndeAssistir(1L)).thenReturn("N/A");

    var result = movieService.buscarPorId(1L);

    assertThat(result.textoFormatado()).contains("🌐");
  }
}
