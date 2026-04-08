package com.market360.api.controller;

import com.market360.agents.CMOAgent;
import com.market360.agents.CMOAgent.AgentEvent;
import com.market360.api.ratelimit.IpExtractor;
import com.market360.api.ratelimit.RateLimitService;
import com.market360.core.model.AnalysisRequest;
import com.market360.core.model.MarketReport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Endpoints REST + SSE pour l'analyse de repos.
 *
 * POST /api/v1/analyze                  → lance une analyse, retourne jobId
 * GET  /api/v1/analyze/{jobId}/stream   → SSE (progression en temps réel, avec replay)
 * GET  /api/v1/analyze/{jobId}/report   → rapport JSON final
 */
@RestController
@RequestMapping("/api/v1")
public class AnalysisController {

    private final CMOAgent         cmoAgent;
    private final RateLimitService rateLimitService;

    // Stockage en mémoire pour MVP — à remplacer par Redis en Phase 2
    private final Map<String, MarketReport>  reports  = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter>    emitters = new ConcurrentHashMap<>();
    // Buffer des events par jobId — permet le replay en cas de connexion tardive
    private final Map<String, List<Map<String, String>>> eventBuffers = new ConcurrentHashMap<>();

    public AnalysisController(CMOAgent cmoAgent, RateLimitService rateLimitService) {
        this.cmoAgent         = cmoAgent;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> startAnalysis(
            @Valid @RequestBody AnalysisRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = IpExtractor.extract(httpRequest);

        if (!rateLimitService.tryConsume(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                        "error",   "limit_exceeded",
                        "message", "3 analyses par jour maximum. Revenez demain !",
                        "remaining", "0"
                    ));
        }

        String jobId = UUID.randomUUID().toString();
        eventBuffers.put(jobId, Collections.synchronizedList(new ArrayList<>()));

        Thread.ofVirtual().name("analysis-" + jobId).start(() -> runAnalysis(jobId, request));

        return ResponseEntity.accepted().body(Map.of(
            "jobId",     jobId,
            "remaining", String.valueOf(rateLimitService.remaining(ip))
        ));
    }

    @GetMapping(value = "/analyze/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnalysis(@PathVariable String jobId) {
        var emitter = new SseEmitter(600_000L);
        emitters.put(jobId, emitter);
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(() -> emitters.remove(jobId));

        // Replay des events déjà émis (connexion tardive)
        var buffer = eventBuffers.get(jobId);
        if (buffer != null) {
            synchronized (buffer) {
                buffer.forEach(event -> sendToEmitter(emitter, event));
            }
        }

        // Si le rapport est déjà terminé, compléter immédiatement
        if (reports.containsKey(jobId)) {
            completeDone(emitter, jobId);
        }

        return emitter;
    }

    @GetMapping("/analyze/{jobId}/report")
    public ResponseEntity<MarketReport> getReport(@PathVariable String jobId) {
        return reports.containsKey(jobId)
                ? ResponseEntity.ok(reports.get(jobId))
                : ResponseEntity.notFound().build();
    }

    // --- Privé ---

    private void runAnalysis(String jobId, AnalysisRequest request) {
        try {
            var report = cmoAgent.orchestrate(request, event -> publishEvent(jobId, event));
            reports.put(jobId, report);
            publishEvent(jobId, new AgentEvent("all", "done", "Rapport prêt"));
            completeDone(emitters.get(jobId), jobId);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
            publishEvent(jobId, new AgentEvent("all", "error", msg));
            completeError(emitters.get(jobId), jobId);
        }
    }

    /** Bufferise l'event ET l'envoie au client connecté. */
    private void publishEvent(String jobId, AgentEvent event) {
        var payload = Map.of(
                "agent",   event.agent(),
                "status",  event.status(),
                "message", event.message()
        );
        var buffer = eventBuffers.get(jobId);
        if (buffer != null) {
            synchronized (buffer) {
                buffer.add(payload);
            }
        }
        var emitter = emitters.get(jobId);
        if (emitter != null) {
            sendToEmitter(emitter, payload);
        }
    }

    private void sendToEmitter(SseEmitter emitter, Map<String, String> payload) {
        try {
            emitter.send(SseEmitter.event().data(payload));
        } catch (IOException ignored) {
            // Le client s'est déconnecté — le buffer conserve les events
        }
    }

    private void completeDone(SseEmitter emitter, String jobId) {
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .data(Map.of("agent", "all", "status", "done", "reportId", jobId)));
        } catch (IOException ignored) {}
        finalizeEmitter(emitter, jobId);
    }

    private void completeError(SseEmitter emitter, String jobId) {
        finalizeEmitter(emitter, jobId);
    }

    private void finalizeEmitter(SseEmitter emitter, String jobId) {
        emitters.remove(jobId);
        eventBuffers.remove(jobId);
        if (emitter == null) return;
        try {
            emitter.complete();
        } catch (Exception ignored) {}
    }
}
