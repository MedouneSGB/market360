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

    public AdCreative generate(ProductBrief brief, GTMPlan gtm, String language) {
        var spec = chatClient.prompt()
                .user(u -> u.text("""
                        Product: {name}
                        USP: {usp}
                        Target segment: {segment}
                        Positioning: {positioning}

                        Generate the creative assets.
                        IMPORTANT: Write ALL JSON string values in {language}. Do NOT prefix ad variants with angle labels.
                        """)
                        .param("name",        brief.name())
                        .param("usp",         brief.usp())
                        .param("segment",     gtm.targetSegment())
                        .param("positioning", gtm.positioningStatement())
                        .param("language",    language))
                .call();

        return converter.convert(spec, AdCreative.class);
    }
}
