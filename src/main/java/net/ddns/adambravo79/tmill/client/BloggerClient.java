/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.client;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.exception.BloggerPublishException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class BloggerClient {

  private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
  private static final String BLOGGER_API_URL = "https://www.googleapis.com/blogger/v3";

  private final String clientId;
  private final String clientSecret;
  private final String refreshToken;
  private final String blogId;
  private final RestClient restClient;

  // ✅ Construtor principal usado pelo Spring
  @Autowired
  public BloggerClient(
      @Value("${google.oauth2.client-id}") String clientId,
      @Value("${google.oauth2.client-secret}") String clientSecret,
      @Value("${google.oauth2.refresh-token}") String refreshToken,
      @Value("${blogger.blog-id}") String blogId) {
    this(clientId, clientSecret, refreshToken, blogId, RestClient.create());
  }

  // Construtor alternativo para testes (permite injetar mock de RestClient)
  public BloggerClient(
      String clientId,
      String clientSecret,
      String refreshToken,
      String blogId,
      RestClient restClient) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.refreshToken = refreshToken;
    this.blogId = blogId;
    this.restClient = restClient;
  }

  private String obterAccessToken() {
    log.info("🔑 Solicitando access token para Blogger blogId={}", blogId);

    String formBody =
        "client_id="
            + clientId
            + "&client_secret="
            + clientSecret
            + "&refresh_token="
            + refreshToken
            + "&grant_type=refresh_token";

    Map<String, Object> response =
        restClient
            .post()
            .uri(TOKEN_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formBody)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});

    if (response == null || !response.containsKey("access_token")) {
      log.error("❌ Falha ao obter access token para Blogger blogId={}", blogId);
      throw new BloggerPublishException("Falha ao obter access token para Blogger");
    }

    log.info("✅ Access token obtido com sucesso para blogId={}", blogId);
    return (String) response.get("access_token");
  }

  public String criarRascunho(String titulo, String conteudo) {
    log.info("📝 Criando rascunho no Blogger blogId={} title={}", blogId, titulo);

    String accessToken = obterAccessToken();
    String conteudoHtml = "<p>" + conteudo.replace("\n", "</p><p>") + "</p>";

    var payload = Map.of("title", titulo, "content", conteudoHtml, "status", "DRAFT");

    Map<String, Object> response;
    try {
      response =
          restClient
              .post()
              .uri(BLOGGER_API_URL + "/blogs/" + blogId + "/posts/")
              .header("Authorization", "Bearer " + accessToken)
              .contentType(MediaType.APPLICATION_JSON)
              .body(payload)
              .retrieve()
              .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      log.error("❌ Erro ao criar rascunho no Blogger blogId={} title={}", blogId, titulo, e);
      throw new BloggerPublishException("Erro ao criar rascunho no Blogger", e);
    }

    if (response == null || !response.containsKey("url")) {
      log.error("❌ Rascunho criado sem URL de retorno blogId={} title={}", blogId, titulo);
      throw new BloggerPublishException("Rascunho criado sem URL de retorno");
    }

    String url = (String) response.get("url");
    log.info("✅ Rascunho criado com sucesso blogId={} title={} url={}", blogId, titulo, url);
    return url;
  }
}
