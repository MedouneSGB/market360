package com.market360.api.controller;

import com.market360.agents.CMOAgent;
import com.market360.core.model.AnalysisRequest;
import com.market360.core.model.MarketReport;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Endpoints REST + SSE pour l'analyse de repos.
 *
 * POST /api/v1/analyze              → lance une analyse, retourne jobId
 * GET  /api/v1/analyze/{jobId}/stream  → SSE (progression en temps réel)
 * GET  /api/v1/analyze/{jobId}/report  → rapport JSON final
 */
@RestController
@RequestMapping("/api/v1")
public class AnalysisController {

    private final CMOAgent cmoAgent;

    // Stockage en mémoire pour MVP — à remplacer par Redis en Phase 2
    private final Map<String, MarketReport> reports = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter>   emitters = new ConcurrentHashMap<>();

    public AnalysisController(CMOAgent cmoAgent) {
        this.cmoAgent = cmoAgent;
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> startAnalysis(
            @Valid @RequestBody AnalysisRequest request
    ) {
        String jobId = UUID.randomUUID().toString();

        // Lancement en virtual thread pour ne pas bloquer le handler HTTP
        Thread.ofVirtual().name("analysis-" + jobId).start(() -> runAnalysis(jobId, request));

        return ResponseEntity.accepted().body(Map.of("jobId", jobId));
    }

    @GetMapping(value = "/analyze/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnalysis(@PathVariable String jobId) {
        var emitter = new SseEmitter(600_000L); // 10 min timeout
        emitters.put(jobId, emitter);
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(() -> emitters.remove(jobId));

        // Si le rapport est déjà prêt (arrivée tardive du client)
        if (reports.containsKey(jobId)) {
            sendDoneEvent(emitter, jobId);
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
        sendEvent(jobId, "analyst",    "running", "Analyse du repo GitHub...");
        try {
            var report = cmoAgent.orchestrate(request);
            reports.put(jobId, report);
            sendEvent(jobId, "all", "done", "Rapport prêt");
            sendDoneEvent(emitters.get(jobId), jobId);
        } catch (Exception e) {
            sendEvent(jobId, "all", "error", e.getMessage());
        }
    }

    private void sendEvent(String jobId, String agent, String status, String message) {
        var emitter = emitters.get(jobId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .data(Map.of("agent", agent, "status", status, "message", message)));
        } catch (IOException ignored) {
            emitters.remove(jobId);
        }
    }

    private void sendDoneEvent(SseEmitter emitter, String jobId) {
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .data(Map.of("agent", "all", "status", "done", "reportId", jobId)));
            emitter.complete();
        } catch (IOException ignored) {}
    }
}
