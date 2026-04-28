/* (c) 2026-2026 */
package net.ddns.adambravo79.tmill.exception;

/** Exceção lançada quando ocorre falha ao publicar ou criar rascunho no Blogger. */
public class BloggerPublishException extends RuntimeException {

    public BloggerPublishException(String message) {
        super(message);
    }

    public BloggerPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
