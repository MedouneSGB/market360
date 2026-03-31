package com.market360.agents.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Wrapper autour de ChatClient.ResponseSpec qui nettoie la réponse LLM
 * avant de la mapper vers un Record Java.
 *
 * Résout le problème des modèles locaux (Ollama/qwen) qui encapsulent
 * le JSON dans des blocs markdown malgré les instructions du prompt.
 */
public class RobustEntityConverter {

    private final ObjectMapper mapper;

    public RobustEntityConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Appelle le LLM, extrait le JSON de la réponse, et le désérialise.
     *
     * @param spec   réponse Spring AI (ChatClient.CallResponseSpec)
     * @param target classe cible (Record)
     */
    public <T> T convert(ChatClient.CallResponseSpec spec, Class<T> target) {
        String raw     = spec.content();
        String cleaned = JsonSanitizer.extractJson(raw);
        try {
            return mapper.readValue(cleaned, target);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Impossible de parser la réponse LLM en " + target.getSimpleName()
                    + "\nJSON nettoyé : " + cleaned
                    + "\nRéponse brute : " + raw, e);
        }
    }
}
