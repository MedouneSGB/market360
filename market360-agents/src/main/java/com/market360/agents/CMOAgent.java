package com.market360.agents;

import com.market360.core.model.AnalysisRequest;
import com.market360.core.model.GTMPlan;
import com.market360.core.model.MarketReport;
import com.market360.core.model.MarketTrends;
import org.springframework.stereotype.Service;

import java.util.concurrent.StructuredTaskScope;
import java.util.function.Consumer;

/**
 * Orchestrateur principal — pipeline en trois phases.
 *
 * Phase 1 (séquentiel) : AnalystAgent → ProductBrief
 * Phase 2 (parallèle)  : StrategistAgent + TrendsAgent en parallèle
 * Phase 3 (séquentiel) : CreativeAgent utilise le GTM de phase 2
 *
 * API Java 25 : StructuredTaskScope.open() + scope.join() qui lève
 * FailedException si une tâche échoue (remplace ShutdownOnFailure).
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

    public MarketReport orchestrate(AnalysisRequest request, Consumer<AgentEvent> onEvent)
            throws Exception {

        // Phase 1 — Analyst (séquentiel, les autres agents en dépendent)
        onEvent.accept(new AgentEvent("analyst", "running", "Analyse du repo GitHub..."));
        var brief = analystAgent.analyze(request.repoUrl());
        onEvent.accept(new AgentEvent("analyst", "done", "Analyse terminée"));

        // Phase 2 — GTM + Trends en parallèle (Java 25 : open() = ShutdownOnFailure)
        onEvent.accept(new AgentEvent("strategist", "running", "Construction du plan GTM..."));
        onEvent.accept(new AgentEvent("trends",     "running", "Analyse des tendances marché..."));

        GTMPlan      gtm;
        MarketTrends trends;
        try (var scope = StructuredTaskScope.open()) {
            var gtmTask    = scope.fork(() -> strategistAgent.buildGTM(brief));
            var trendsTask = scope.fork(() -> trendsAgent.fetch(brief, request.market(), request.location()));
            scope.join();   // lève FailedException si l'un des agents échoue
            gtm    = gtmTask.get();
            trends = trendsTask.get();
        }

        onEvent.accept(new AgentEvent("strategist", "done", "Plan GTM terminé"));
        onEvent.accept(new AgentEvent("trends",     "done", "Tendances collectées"));

        // Phase 3 — Creative (nécessite le GTM)
        onEvent.accept(new AgentEvent("creative", "running", "Génération du copy créatif..."));
        var creative = creativeAgent.generate(brief, gtm);
        onEvent.accept(new AgentEvent("creative", "done", "Assets créatifs générés"));

        return MarketReport.from(brief, gtm, trends, creative);
    }

    public record AgentEvent(String agent, String status, String message) {}
}
