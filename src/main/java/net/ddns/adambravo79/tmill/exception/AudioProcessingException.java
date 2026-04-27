/* (c) 2026 */
package net.ddns.adambravo79.tmill.exception;

public class AudioProcessingException extends RuntimeException {

    private final String contexto;

    public AudioProcessingException(String message) {
        super(message);
        this.contexto = null;
    }

    public AudioProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.contexto = null;
    }

    public AudioProcessingException(String message, String contexto, Throwable cause) {
        super(message, cause);
        this.contexto = contexto;
    }

    public String getContexto() {
        return contexto;
    }
}
