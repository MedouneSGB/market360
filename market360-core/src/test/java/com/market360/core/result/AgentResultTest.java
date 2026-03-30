package com.market360.core.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentResultTest {

    @Test
    void successOrElseThrow() {
        AgentResult<String> result = AgentResult.success("ok");
        assertEquals("ok", result.orElseThrow());
    }

    @Test
    void failureOrElseThrowLancesException() {
        AgentResult<String> result = AgentResult.failure("erreur réseau");
        assertThrows(AgentException.class, result::orElseThrow);
    }

    @Test
    void mapSurSucces() {
        AgentResult<Integer> result = AgentResult.success("hello").map(String::length);
        assertEquals(5, result.orElseThrow());
    }

    @Test
    void mapSurFailurePropageLErreur() {
        AgentResult<Integer> result = AgentResult.<String>failure("ko").map(String::length);
        assertTrue(result instanceof AgentResult.Failure<?>);
    }

    @Test
    void toOptional() {
        assertTrue(AgentResult.success("x").toOptional().isPresent());
        assertTrue(AgentResult.failure("err").toOptional().isEmpty());
    }
}
