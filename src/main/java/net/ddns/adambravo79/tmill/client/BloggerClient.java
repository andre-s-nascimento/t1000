package net.ddns.adambravo79.tmill.client;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Cliente para a API v3 do Google Blogger.
 * Usa refresh_token salvo em variável de ambiente para obter access tokens.
 * Não requer nenhum fluxo interativo de login — autorização feita uma única
 * vez.
 */
@Log4j2
@Component
public class BloggerClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String BLOGGER_API_URL = "https://www.googleapis.com/blogger/v3";

    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String blogId;

    // RestClient genérico (sem baseUrl fixa, pois usamos URLs completas aqui)
    private final RestClient restClient = RestClient.create();

    public BloggerClient(
            @Value("${google.oauth2.client-id}") String clientId,
            @Value("${google.oauth2.client-secret}") String clientSecret,
            @Value("${google.oauth2.refresh-token}") String refreshToken,
            @Value("${blogger.blog-id}") String blogId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.blogId = blogId;
        log.info("BloggerClient init — clientId={}, refreshToken={}, blogId={}",
                clientId.substring(0, 6) + "...",
                refreshToken.isEmpty() ? "VAZIO!" : refreshToken.substring(0, 6) + "...",
                blogId);
    }

    /**
     * Obtém um access_token fresco usando o refresh_token salvo.
     * O access_token do Google expira em 1h, por isso sempre renovamos.
     */
    private String obterAccessToken() {
        log.debug("Blogger: Renovando access token via refresh_token...");
        try {
            String formBody = "client_id=" + clientId
                    + "&client_secret=" + clientSecret
                    + "&refresh_token=" + refreshToken
                    + "&grant_type=refresh_token";

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("access_token")) {
                log.error("Blogger: Resposta de token inválida: {}", response);
                return null;
            }
            return (String) response.get("access_token");

        } catch (Exception e) {
            log.error("Blogger: Falha ao renovar access token", e);
            return null;
        }
    }

    /**
     * Cria um rascunho no Blogger com título e conteúdo HTML.
     *
     * @param titulo   Título do post (pode ser gerado a partir da transcrição)
     * @param conteudo Texto da transcrição (será wrapped em parágrafo HTML)
     * @return URL do rascunho criado, ou null em caso de falha
     */
    public String criarRascunho(String titulo, String conteudo) {
        String accessToken = obterAccessToken();
        if (accessToken == null) {
            return null;
        }

        log.info("Blogger: Criando rascunho - '{}'", titulo);
        try {
            // O Blogger aceita HTML no conteúdo
            String conteudoHtml = "<p>" + conteudo.replace("\n", "</p><p>") + "</p>";

            var payload = Map.of(
                    "title", titulo,
                    "content", conteudoHtml,
                    "status", "DRAFT" // Garante que é rascunho, não publicado
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(BLOGGER_API_URL + "/blogs/" + blogId + "/posts/")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("url")) {
                String url = (String) response.get("url");
                log.info("Blogger: Rascunho criado com sucesso. URL: {}", url);
                return url;
            }

            log.warn("Blogger: Rascunho criado mas sem URL na resposta: {}", response);
            return "https://www.blogger.com"; // fallback genérico

        } catch (Exception e) {
            log.error("Blogger: Falha ao criar rascunho", e);
            return null;
        }
    }
}