package com.market360.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market360.agents.util.RobustEntityConverter;
import com.market360.core.model.MarketTrends;
import com.market360.core.model.ProductBrief;
import com.market360.skills.SkillLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class TrendsAgent {

    private final ChatClient            chatClient;
    private final RobustEntityConverter converter;

    public TrendsAgent(ChatClient.Builder builder, ObjectMapper mapper) {
        this.chatClient = builder
                .defaultSystem(SkillLoader.load("geo-market-analysis"))
                .build();
        this.converter = new RobustEntityConverter(mapper);
    }

    public MarketTrends fetch(ProductBrief brief, String market, String location) {
        String scope    = market == null ? "global" : market;
        String geoLabel = "global".equals(scope) ? "worldwide" : location;

        var spec = chatClient.prompt()
                .user(u -> u.text("""
                        Produit : {name} — {description}
                        Stack : {stack}
                        Marché : {scope} ({location})

                        Analyse le marché.
                        """)
                        .param("name",        brief.name())
                        .param("description", brief.description())
                        .param("stack",       brief.stack())
                        .param("scope",       scope)
                        .param("location",    geoLabel))
                .call();

        return converter.convert(spec, MarketTrends.class);
    }
}
