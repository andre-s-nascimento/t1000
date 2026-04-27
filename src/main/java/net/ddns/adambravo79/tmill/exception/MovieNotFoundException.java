/* (c) 2026 */
package net.ddns.adambravo79.tmill.exception;

/** Exceção lançada quando um filme não é encontrado no TMDB. */
public class MovieNotFoundException extends RuntimeException {

  public MovieNotFoundException(String message) {
    super(message);
  }

  public MovieNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
