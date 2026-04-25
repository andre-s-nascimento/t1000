/* (c) 2026 */
package net.ddns.adambravo79.tmill.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
    log.error("Erro inesperado", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            Map.of(
                "timestamp", LocalDateTime.now(),
                "error", "Erro interno",
                "message", ex.getMessage()));
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<Map<String, Object>> handleIO(IOException ex) {
    log.error("Erro de I/O", ex);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            Map.of(
                "timestamp", LocalDateTime.now(),
                "error", "Erro de entrada/saída",
                "message", ex.getMessage()));
  }

  // Você pode adicionar outros handlers conforme necessário
}
