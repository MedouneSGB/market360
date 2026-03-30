package com.market360.agents;

import com.market360.core.model.GTMPlan;
import com.market360.core.model.ProductBrief;
import com.market360.skills.SkillLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Construit un plan Go-To-Market à partir du ProductBrief.
 *
 * Modèle : claude-sonnet-4-6 (analyse stratégique complexe)
 */
@Service
public class StrategistAgent {

    private final ChatClient chatClient;

    public StrategistAgent(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SkillLoader.load("gtm-strategy"))
                .build();
    }

    public GTMPlan buildGTM(ProductBrief brief) {
        return chatClient.prompt()
                .user(u -> u.text("""
                        Produit : {name}
                        Description : {description}
                        Stack : {stack}
                        Audience : {audience}
                        USP : {usp}

                        Construis le plan GTM.
                        """)
                        .param("name", brief.name())
                        .param("description", brief.description())
                        .param("stack", brief.stack())
                        .param("audience", brief.audience())
                        .param("usp", brief.usp()))
                .call()
                .entity(GTMPlan.class);
    }
}
