package com.market360.core.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AgentStatusTest {

    @Test
    void patternMatchingExhaustif() {
        AgentStatus running = new AgentStatus.Running("analyst", Instant.now());
        AgentStatus done    = new AgentStatus.Done("analyst", Duration.ofSeconds(3));
        AgentStatus failed  = new AgentStatus.Failed("analyst", "timeout", new RuntimeException());

        assertEquals("En cours", describe(running));
        assertEquals("Terminé", describe(done));
        assertEquals("Erreur : timeout", describe(failed));
    }

    private String describe(AgentStatus s) {
        return switch (s) {
            case AgentStatus.Running r -> "En cours";
            case AgentStatus.Done d    -> "Terminé";
            case AgentStatus.Failed f  -> "Erreur : " + f.reason();
        };
    }
}
