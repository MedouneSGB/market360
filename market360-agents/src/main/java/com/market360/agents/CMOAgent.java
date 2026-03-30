package com.market360.agents;

import com.market360.core.model.AnalysisRequest;
import com.market360.core.model.GTMPlan;
import com.market360.core.model.MarketReport;
import org.springframework.stereotype.Service;

import java.util.concurrent.StructuredTaskScope;
import java.util.function.Consumer;

/**
 * Orchestrateur principal — pipeline en deux phases avec un seul StructuredTaskScope.
 *
 * Phase 1 (séquentiel) : AnalystAgent → ProductBrief
 * Phase 2 (parallèle)  : StrategistAgent + TrendsAgent en parallèle
 *                        CreativeAgent attend le GTM à l'intérieur de sa propre tâche
 */
@Service
public class CMOAgent {

    private final AnalystAgent    analystAgent;
    private final StrategistAgent strategistAgent;
    private final TrendsAgent     trendsAgent;
    private final CreativeAgent   creativeAgent;

    public CMOAgent(
            AnalystAgent    analystAgent,
            StrategistAgent strategistAgent,
            TrendsAgent     trendsAgent,
            CreativeAgent   creativeAgent
    ) {
        this.analystAgent    = analystAgent;
        this.strategistAgent = strategistAgent;
        this.trendsAgent     = trendsAgent;
        this.creativeAgent   = creativeAgent;
    }

    /**
     * @param onEvent callback pour les événements de progression (agent, status, message)
     */
    public MarketReport orchestrate(AnalysisRequest request, Consumer<AgentEvent> onEvent)
            throws InterruptedException {

        // Phase 1 — analyse séquentielle (les autres agents en ont besoin)
        onEvent.accept(new AgentEvent("analyst", "running", "Analyse du repo GitHub..."));
        var brief = analystAgent.analyze(request.repoUrl());
        onEvent.accept(new AgentEvent("analyst", "done", "Analyse terminée"));

        // Phase 2 — StrategistAgent, TrendsAgent, et CreativeAgent (qui attend GTM)
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            onEvent.accept(new AgentEvent("strategist", "running", "Construction du plan GTM..."));
            onEvent.accept(new AgentEvent("trends",     "running", "Analyse des tendances marché..."));

            var gtmTask    = scope.fork(() -> strategistAgent.buildGTM(brief));
            var trendsTask = scope.fork(() -> trendsAgent.fetch(brief, request.market(), request.location()));

            // CreativeAgent attend GTM dans sa propre tâche — pas de scope imbriqué
            var creativeTask = scope.fork(() -> {
                GTMPlan gtm = gtmTask.get();  // attend uniquement GTM, pas trends
                onEvent.accept(new AgentEvent("creative", "running", "Génération du copy créatif..."));
                return creativeAgent.generate(brief, gtm);
            });

            scope.join().throwIfFailed();

            onEvent.accept(new AgentEvent("strategist", "done", "Plan GTM terminé"));
            onEvent.accept(new AgentEvent("trends",     "done", "Tendances collectées"));
            onEvent.accept(new AgentEvent("creative",   "done", "Assets créatifs générés"));

            return MarketReport.from(brief, gtmTask.get(), trendsTask.get(), creativeTask.get());
        }
    }

    public record AgentEvent(String agent, String status, String message) {}
}
