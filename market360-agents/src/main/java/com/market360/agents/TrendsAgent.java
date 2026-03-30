package com.market360.agents;

import com.market360.core.model.MarketTrends;
import com.market360.core.model.ProductBrief;
import com.market360.skills.SkillLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Collecte les tendances marché locales ou internationales.
 *
 * Modèle : claude-haiku-4-5-20251001 (recherche rapide)
 */
@Service
public class TrendsAgent {

    private final ChatClient chatClient;

    public TrendsAgent(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SkillLoader.load("geo-market-analysis"))
                .build();
    }

    public MarketTrends fetch(ProductBrief brief, String market, String location) {
        String scope    = market == null ? "global" : market;
        String geoLabel = "global".equals(scope) ? "worldwide" : location;

        return chatClient.prompt()
                .user(u -> u.text("""
                        Produit : {name} — {description}
                        Stack : {stack}
                        Marché : {scope} ({location})

                        Analyse le marché.
                        """)
                        .param("name", brief.name())
                        .param("description", brief.description())
                        .param("stack", brief.stack())
                        .param("scope", scope)
                        .param("location", geoLabel))
                .call()
                .entity(MarketTrends.class);
    }
}
