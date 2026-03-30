package com.market360.agents;

import com.market360.core.model.AdCreative;
import com.market360.core.model.GTMPlan;
import com.market360.core.model.ProductBrief;
import com.market360.skills.SkillLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Génère le copy publicitaire et un brief visuel.
 *
 * Modèle : claude-haiku-4-5-20251001 (génération rapide)
 */
@Service
public class CreativeAgent {

    private final ChatClient chatClient;

    public CreativeAgent(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SkillLoader.load("ad-creative"))
                .build();
    }

    public AdCreative generate(ProductBrief brief, GTMPlan gtm) {
        return chatClient.prompt()
                .user(u -> u.text("""
                        Produit : {name}
                        USP : {usp}
                        Segment cible : {segment}
                        Positionnement : {positioning}

                        Génère les assets créatifs.
                        """)
                        .param("name", brief.name())
                        .param("usp", brief.usp())
                        .param("segment", gtm.targetSegment())
                        .param("positioning", gtm.positioningStatement()))
                .call()
                .entity(AdCreative.class);
    }
}
