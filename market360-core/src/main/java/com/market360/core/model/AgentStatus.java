package com.market360.core.model;

import java.time.Duration;
import java.time.Instant;

/**
 * État d'un agent — sealed interface avec pattern matching exhaustif.
 *
 * Usage :
 * <pre>{@code
 * String describe(AgentStatus s) {
 *     return switch (s) {
 *         case AgentStatus.Running r  -> "En cours depuis " + r.startedAt();
 *         case AgentStatus.Done d     -> "Terminé en " + d.elapsed().toSeconds() + "s";
 *         case AgentStatus.Failed f   -> "Erreur : " + f.reason();
 *     };
 * }
 * }</pre>
 */
public sealed interface AgentStatus {

    record Running(String agentName, Instant startedAt) implements AgentStatus {}

    record Done(String agentName, Duration elapsed) implements AgentStatus {}

    record Failed(String agentName, String reason, Throwable cause) implements AgentStatus {}
}
