package com.market360.agents;

import com.market360.core.model.ProductBrief;
import com.market360.skills.SkillLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Analyse un repo GitHub et retourne un ProductBrief structuré.
 *
 * Modèle : claude-sonnet-4-6 (analyse complexe)
 */
@Service
public class AnalystAgent {

    private final ChatClient chatClient;

    public AnalystAgent(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SkillLoader.load("repo-analysis"))
                .build();
    }

    public ProductBrief analyze(String repoUrl) {
        return chatClient.prompt()
                .user(u -> u.text("Analyse ce repo GitHub : {url}").param("url", repoUrl))
                .call()
                .entity(ProductBrief.class);
    }
}
