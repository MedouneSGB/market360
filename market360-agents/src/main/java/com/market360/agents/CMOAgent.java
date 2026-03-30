package com.market360.agents;

import com.market360.core.model.AnalysisRequest;
import com.market360.core.model.MarketReport;
import org.springframework.stereotype.Service;

import java.util.concurrent.StructuredTaskScope;

/**
 * Orchestrateur principal — lance les 4 agents en parallèle via StructuredTaskScope.
 *
 * Pattern : tous les agents tournent en parallèle (virtual threads).
 * Si l'un échoue, ShutdownOnFailure annule les autres immédiatement.
 */
@Service
public class CMOAgent {

    private final AnalystAgent   analystAgent;
    private final StrategistAgent strategistAgent;
    private final TrendsAgent    trendsAgent;
    private final CreativeAgent  creativeAgent;

    public CMOAgent(
            AnalystAgent   analystAgent,
            StrategistAgent strategistAgent,
            TrendsAgent    trendsAgent,
            CreativeAgent  creativeAgent
    ) {
        this.analystAgent    = analystAgent;
        this.strategistAgent = strategistAgent;
        this.trendsAgent     = trendsAgent;
        this.creativeAgent   = creativeAgent;
    }

    public MarketReport orchestrate(AnalysisRequest request) throws InterruptedException {
        // Phase 1 : analyse du repo (bloquant — les autres agents en dépendent)
        var brief = analystAgent.analyze(request.repoUrl());

        // Phase 2 : StrategistAgent, TrendsAgent et CreativeAgent en parallèle
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            var gtmTask     = scope.fork(() -> strategistAgent.buildGTM(brief));
            var trendsTask  = scope.fork(() -> trendsAgent.fetch(brief, request.market(), request.location()));

            // Attendre GTM pour alimenter le CreativeAgent
            scope.join().throwIfFailed();

            var gtm = gtmTask.get();

            try (var creativeScope = new StructuredTaskScope.ShutdownOnFailure()) {
                var creativeTask = creativeScope.fork(() -> creativeAgent.generate(brief, gtm));
                creativeScope.join().throwIfFailed();

                return MarketReport.from(brief, gtm, trendsTask.get(), creativeTask.get());
            }
        }
    }
}
