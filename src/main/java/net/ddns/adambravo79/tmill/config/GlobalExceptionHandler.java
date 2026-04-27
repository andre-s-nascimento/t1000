/* (c) 2026-2026 */
package net.ddns.adambravo79.tmill.config;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.exception.AudioProcessingException;
import net.ddns.adambravo79.tmill.exception.BloggerPublishException;
import net.ddns.adambravo79.tmill.exception.MovieNotFoundException;
import net.ddns.adambravo79.tmill.exception.TelegramFileException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final String MESSAGE = "message";
  private static final String ERROR = "error";
  private static final String TIMESTAMP = "timestamp";

  @ExceptionHandler(AudioProcessingException.class)
  public ResponseEntity<Map<String, Object>> handleAudio(AudioProcessingException ex) {
    log.error("Erro no processamento de áudio", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            Map.of(
                TIMESTAMP, LocalDateTime.now(),
                ERROR, "AudioProcessingException",
                MESSAGE, ex.getMessage()));
  }

  @ExceptionHandler(TelegramFileException.class)
  public ResponseEntity<Map<String, Object>> handleTelegramFile(TelegramFileException ex) {
    log.error("Erro ao manipular arquivo do Telegram", ex);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            Map.of(
                TIMESTAMP, LocalDateTime.now(),
                ERROR, "TelegramFileException",
                MESSAGE, ex.getMessage()));
  }

  @ExceptionHandler(MovieNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleMovieNotFound(MovieNotFoundException ex) {
    log.warn("Filme não encontrado", ex);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                TIMESTAMP, LocalDateTime.now(),
                ERROR, "MovieNotFoundException",
                MESSAGE, ex.getMessage()));
  }

  @ExceptionHandler(BloggerPublishException.class)
  public ResponseEntity<Map<String, Object>> handleBlogger(BloggerPublishException ex) {
    log.error("Erro ao publicar no Blogger", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            Map.of(
                TIMESTAMP, LocalDateTime.now(),
                ERROR, "BloggerPublishException",
                MESSAGE, ex.getMessage()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
    log.error("Erro inesperado", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            Map.of(
                TIMESTAMP, LocalDateTime.now(),
                ERROR, "RuntimeException",
                MESSAGE, ex.getMessage()));
  }
}
