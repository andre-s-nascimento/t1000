/* (c) 2026 */
package net.ddns.adambravo79.tmill.telegram;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RetryPolicy {

  private static final int MAX_RETRIES = 3;
  private static final long BACKOFF_MS = 1000;

  public <T> T execute(Callable<T> action) throws Exception {
    int attempt = 0;

    while (true) {
      try {
        return action.call();

      } catch (Exception e) {
        attempt++;

        if (!shouldRetry(e) || attempt >= MAX_RETRIES) {
          throw e;
        }

        log.warn("Retry attempt {} due to {}", attempt, e.getMessage());

        Thread.sleep(BACKOFF_MS * attempt); // backoff linear simples
      }
    }
  }

  private boolean shouldRetry(Exception e) {
    String msg = e.getMessage() != null ? e.getMessage() : "";

    return msg.contains("429") // rate limit
        || msg.toLowerCase().contains("timeout")
        || e instanceof java.io.IOException;
  }
}
