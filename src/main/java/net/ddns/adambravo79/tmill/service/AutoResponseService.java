/* (c) 2026 | 20/05/2026 */
package net.ddns.adambravo79.tmill.service;

import java.util.*;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AutoResponseService {

    private final Map<String, AutoResponseRule> triggerToRule = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${auto.response.enabled:false}")
    private boolean enabled;

    @Value("${auto.response.file:classpath:auto-responses.json}")
    private String configFile;

    private final ResourceLoader resourceLoader;

    public AutoResponseService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        if (enabled) {
            loadResponses();
        }
    }

    public void loadResponses() {
        try {
            Resource resource = resourceLoader.getResource(configFile);
            if (!resource.exists()) {
                log.warn("Arquivo de respostas automáticas não encontrado: {}", configFile);
                return;
            }

            Map<String, Map<String, Object>> config =
                    mapper.readValue(resource.getInputStream(), new TypeReference<>() {});
            triggerToRule.clear();

            for (Map.Entry<String, Map<String, Object>> entry : config.entrySet()) {
                List<String> triggers = (List<String>) entry.getValue().get("triggers");
                String response = (String) entry.getValue().get("response");
                String animation = (String) entry.getValue().get("animation");
                if (triggers != null && response != null) {
                    for (String trigger : triggers) {
                        if (trigger != null && !trigger.isBlank()) {
                            triggerToRule.put(
                                    trigger.toLowerCase(),
                                    new AutoResponseRule(response, animation));
                        }
                    }
                }
            }

            log.info("✅ Carregadas {} regras de resposta automática", triggerToRule.size());
            log.debug("Triggers carregados: {}", triggerToRule.keySet());
        } catch (Exception e) {
            log.error("Falha ao carregar respostas automáticas", e);
        }
    }

    /** Verifica se o texto contém a palavra exata (ou frase) – não substring genérica. */
    private boolean containsExactWord(String text, String word) {
        // Usa boundaries de palavra para evitar "dia" dentro de "diaNielsen"
        Pattern pattern =
                Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(text).find();
    }

    public Optional<AutoResponseRule> getResponseRule(String message) {
        if (!enabled || message == null || message.isBlank()) {
            return Optional.empty();
        }

        String lowerMsg = message.toLowerCase();
        log.debug("Verificando mensagem: '{}'", lowerMsg);

        // Para evitar falsos positivos, ordena triggers por tamanho (mais específicos primeiro)
        List<Map.Entry<String, AutoResponseRule>> sorted =
                new ArrayList<>(triggerToRule.entrySet());
        sorted.sort((a, b) -> b.getKey().length() - a.getKey().length());

        for (Map.Entry<String, AutoResponseRule> entry : sorted) {
            String trigger = entry.getKey();
            // Ignora triggers muito curtos (menos de 3 caracteres) – opcional, evita "a", "de",
            // etc.
            if (trigger.length() < 3) {
                continue;
            }
            // Verifica se a mensagem contém a palavra/frase exata
            if (containsExactWord(lowerMsg, trigger)) {
                log.info("✅ Trigger '{}' ativado pela mensagem: '{}'", trigger, lowerMsg);
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public void reload() {
        loadResponses();
    }
}
