/* (c) 2026 */
package net.ddns.adambravo79.tmill.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Cache em memória que guarda a última transcrição refinada por chatId. Necessário para que, quando
 * o usuário clicar em "Publicar como Rascunho", o TelegramController saiba qual texto associar ao
 * clique.
 *
 * <p>Simples e intencional: não persiste, não tem TTL complexo. Se o bot reiniciar, o estado se
 * perde — comportamento aceitável para este caso.
 */
@Slf4j
@Component
public class TranscricaoCache {

    // chatId → texto refinado da última transcrição
    private final ConcurrentHashMap<Long, String> cache = new ConcurrentHashMap<>();

    public void salvar(long chatId, String textoRefinado) {
        cache.put(chatId, textoRefinado);
        log.debug("Cache: Transcrição salva para chatId={}", chatId);
    }

    public String recuperar(long chatId) {
        return cache.get(chatId);
    }

    public void remover(long chatId) {
        cache.remove(chatId);
        log.debug("Cache: Transcrição removida para chatId={}", chatId);
    }

    public boolean existe(long chatId) {
        return cache.containsKey(chatId);
    }
}
