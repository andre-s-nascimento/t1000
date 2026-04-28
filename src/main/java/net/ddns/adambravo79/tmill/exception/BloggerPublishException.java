/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.exception;

/**
 * Exceção lançada quando ocorre falha ao publicar ou criar rascunho no Blogger.
 *
 * <p>Permite incluir contexto adicional (ex.: blogId, título do post) para enriquecer os logs e
 * facilitar a depuração.
 */
public class BloggerPublishException extends RuntimeException {

  private final String contexto;

  public BloggerPublishException(String message) {
    super(message);
    this.contexto = null;
  }

  public BloggerPublishException(String message, Throwable cause) {
    super(message, cause);
    this.contexto = null;
  }

  public BloggerPublishException(String message, String contexto, Throwable cause) {
    super(message, cause);
    this.contexto = contexto;
  }

  public String getContexto() {
    return contexto;
  }

  @Override
  public String toString() {
    return "BloggerPublishException{message=" + getMessage() + ", contexto=" + contexto + "}";
  }
}
