/* (c) 2026-2026 */
package net.ddns.adambravo79.tmill.client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import net.ddns.adambravo79.tmill.exception.BloggerPublishException;

class BloggerClientTest {

  private RestClient restClient;
  private RestClient.RequestBodyUriSpec uriSpec;

  // ✅ Tipados como RequestBodySpec — compatível com o retorno real de body()
  private RestClient.RequestBodySpec tokenBodySpec;
  private RestClient.RequestBodySpec postBodySpec;

  // ✅ Também RequestBodySpec: é superinterface de RequestHeadersSpec e
  // é o tipo que body() realmente declara como retorno
  private RestClient.RequestBodySpec tokenAfterBodySpec;
  private RestClient.RequestBodySpec postAfterBodySpec;

  private RestClient.ResponseSpec tokenResponseSpec;
  private RestClient.ResponseSpec postResponseSpec;

  @BeforeEach
  void setUp() {
    restClient = mock(RestClient.class);
    uriSpec = mock(RestClient.RequestBodyUriSpec.class);
    tokenBodySpec = mock(RestClient.RequestBodySpec.class);
    postBodySpec = mock(RestClient.RequestBodySpec.class);
    tokenAfterBodySpec = mock(RestClient.RequestBodySpec.class);
    postAfterBodySpec = mock(RestClient.RequestBodySpec.class);
    tokenResponseSpec = mock(RestClient.ResponseSpec.class);
    postResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.post()).thenReturn(uriSpec);
  }

  @SuppressWarnings("unchecked")
  private void stubFluxoToken(Map<String, Object> retorno) {
    when(uriSpec.uri("https://oauth2.googleapis.com/token")).thenReturn(tokenBodySpec);
    when(tokenBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .thenReturn(tokenBodySpec);
    // ✅ Mesmo tipo de retorno que body() declara — sem conflito de tipo
    when(tokenBodySpec.body(anyString())).thenReturn(tokenAfterBodySpec);
    when(tokenAfterBodySpec.retrieve()).thenReturn(tokenResponseSpec);
    when(tokenResponseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(retorno);
  }

  @SuppressWarnings("unchecked")
  private void stubFluxoPost(String blogId, Map<String, Object> retorno) {
    when(uriSpec.uri("https://www.googleapis.com/blogger/v3/blogs/" + blogId + "/posts/"))
        .thenReturn(postBodySpec);
    when(postBodySpec.header(eq("Authorization"), anyString())).thenReturn(postBodySpec);
    when(postBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(postBodySpec);
    // ✅ Mesmo tipo de retorno que body() declara — sem conflito de tipo
    when(postBodySpec.body(anyMap())).thenReturn(postAfterBodySpec);
    when(postAfterBodySpec.retrieve()).thenReturn(postResponseSpec);
    when(postResponseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(retorno);
  }

  private BloggerClient criarClient() {
    return new BloggerClient("id", "secret", "refresh", "blog123", restClient);
  }

  @Test
  void deveCriarRascunhoComSucesso() {
    stubFluxoToken(Map.of("access_token", "token123"));
    stubFluxoPost("blog123", Map.of("url", "http://blogger.com/post/1"));

    String url = criarClient().criarRascunho("Titulo", "Conteudo");

    assertThat(url).isEqualTo("http://blogger.com/post/1");
  }

  @Test
  void deveFalharQuandoTokenInvalido() {
    stubFluxoToken(Map.of()); // sem access_token

    assertThatExceptionOfType(BloggerPublishException.class)
        .isThrownBy(() -> criarClient().criarRascunho("Titulo", "Conteudo"))
        .withMessageContaining("Falha ao obter access token");
  }

  @Test
  void deveFalharQuandoRespostaSemUrl() {
    stubFluxoToken(Map.of("access_token", "token123"));
    stubFluxoPost("blog123", Map.of()); // sem url

    assertThatExceptionOfType(BloggerPublishException.class)
        .isThrownBy(() -> criarClient().criarRascunho("Titulo", "Conteudo"))
        .withMessageContaining("Rascunho criado sem URL");
  }
}
