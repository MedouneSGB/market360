package com.market360.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market360.agents.util.RobustEntityConverter;
import com.market360.core.model.AdCreative;
import com.market360.core.model.GTMPlan;
import com.market360.core.model.ProductBrief;
import com.market360.skills.SkillLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class CreativeAgent {

    private final ChatClient            chatClient;
    private final RobustEntityConverter converter;

    public CreativeAgent(ChatClient.Builder builder, ObjectMapper mapper) {
        this.chatClient = builder
                .defaultSystem(SkillLoader.load("ad-creative"))
                .build();
        this.converter = new RobustEntityConverter(mapper);
    }

    public AdCreative generate(ProductBrief brief, GTMPlan gtm) {
        var spec = chatClient.prompt()
                .user(u -> u.text("""
                        Produit : {name}
                        USP : {usp}
                        Segment cible : {segment}
                        Positionnement : {positioning}

                        Génère les assets créatifs.
                        """)
                        .param("name",        brief.name())
                        .param("usp",         brief.usp())
                        .param("segment",     gtm.targetSegment())
                        .param("positioning", gtm.positioningStatement()))
                .call();

        return converter.convert(spec, AdCreative.class);
    }
}
