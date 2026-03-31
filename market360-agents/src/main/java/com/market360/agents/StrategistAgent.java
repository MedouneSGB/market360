package com.market360.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market360.agents.util.RobustEntityConverter;
import com.market360.core.model.GTMPlan;
import com.market360.core.model.ProductBrief;
import com.market360.skills.SkillLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class StrategistAgent {

    private final ChatClient            chatClient;
    private final RobustEntityConverter converter;

    public StrategistAgent(ChatClient.Builder builder, ObjectMapper mapper) {
        this.chatClient = builder
                .defaultSystem(SkillLoader.load("gtm-strategy"))
                .build();
        this.converter = new RobustEntityConverter(mapper);
    }

    public GTMPlan buildGTM(ProductBrief brief) {
        var spec = chatClient.prompt()
                .user(u -> u.text("""
                        Produit : {name}
                        Description : {description}
                        Stack : {stack}
                        Audience : {audience}
                        USP : {usp}

                        Construis le plan GTM.
                        """)
                        .param("name",        brief.name())
                        .param("description", brief.description())
                        .param("stack",       brief.stack())
                        .param("audience",    brief.audience())
                        .param("usp",         brief.usp()))
                .call();

        return converter.convert(spec, GTMPlan.class);
    }
}
