/* (c) 2026-2026 */
package net.ddns.adambravo79.tmill.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import net.ddns.adambravo79.tmill.exception.AudioProcessingException;
import net.ddns.adambravo79.tmill.exception.BloggerPublishException;
import net.ddns.adambravo79.tmill.exception.MovieNotFoundException;
import net.ddns.adambravo79.tmill.exception.TelegramFileException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void deveTratarAudioProcessingException() {
    ResponseEntity<Map<String, Object>> response =
        handler.handleAudio(new AudioProcessingException("Falha no áudio"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).containsEntry("error", "AudioProcessingException");
    assertThat(response.getBody()).containsEntry("message", "Falha no áudio");
    assertThat(response.getBody()).containsKey("timestamp");
  }

  @Test
  void deveTratarTelegramFileException() {
    ResponseEntity<Map<String, Object>> response =
        handler.handleTelegramFile(new TelegramFileException("Erro no arquivo", null));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).containsEntry("error", "TelegramFileException");
    assertThat(response.getBody()).containsEntry("message", "Erro no arquivo");
    assertThat(response.getBody()).containsKey("timestamp");
  }

  @Test
  void deveTratarMovieNotFoundException() {
    ResponseEntity<Map<String, Object>> response =
        handler.handleMovieNotFound(new MovieNotFoundException("Filme não encontrado"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).containsEntry("error", "MovieNotFoundException");
    assertThat(response.getBody()).containsEntry("message", "Filme não encontrado");
    assertThat(response.getBody()).containsKey("timestamp");
  }

  @Test
  void deveTratarBloggerPublishException() {
    ResponseEntity<Map<String, Object>> response =
        handler.handleBlogger(new BloggerPublishException("Erro no Blogger"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).containsEntry("error", "BloggerPublishException");
    assertThat(response.getBody()).containsEntry("message", "Erro no Blogger");
    assertThat(response.getBody()).containsKey("timestamp");
  }

  @Test
  void deveTratarRuntimeException() {
    ResponseEntity<Map<String, Object>> response =
        handler.handleRuntime(new RuntimeException("Erro genérico"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).containsEntry("error", "RuntimeException");
    assertThat(response.getBody()).containsEntry("message", "Erro genérico");
    assertThat(response.getBody()).containsKey("timestamp");
  }
}
