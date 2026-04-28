/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.telegram;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MetricsService {

    private final ConcurrentHashMap<String, AtomicLong> success = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> error = new ConcurrentHashMap<>();

    public void success(String key) {
        success.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }

    public void error(String key) {
        error.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }

    public long getSuccess(String key) {
        return success.getOrDefault(key, new AtomicLong(0)).get();
    }

    public long getError(String key) {
        return error.getOrDefault(key, new AtomicLong(0)).get();
    }
}
