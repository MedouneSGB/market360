package com.market360.api.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Résout l'ambiguïté entre anthropicChatModel et ollamaChatModel
 * en fournissant un ChatClient.Builder explicite par profil.
 *
 * L'autoconfig ChatClientAutoConfiguration a @ConditionalOnMissingBean
 * sur ChatClient.Builder — notre bean a donc priorité.
 *
 * local  → Ollama (qwen2.5-coder:7b, aucune clé requise)
 * défaut → ChatClientAutoConfiguration utilise Anthropic seul
 *          (désactiver Ollama via spring.ai.ollama.chat.enabled=false)
 */
@Configuration
public class ChatModelConfig {

    @Bean
    @Profile("local")
    @ConditionalOnMissingBean(ChatClient.Builder.class)
    public ChatClient.Builder localChatClientBuilder(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel);
    }
}
