package com.market360.core.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Rapport final agrégé par le CMOAgent à partir des 4 agents.
 */
public record MarketReport(
        String id,
        Instant generatedAt,
        ProductBrief product,
        GTMPlan gtmPlan,
        MarketTrends trends,
        AdCreative creative
) {
    /** Fabrique depuis les résultats des 4 agents */
    public static MarketReport from(
            ProductBrief product,
            GTMPlan gtmPlan,
            MarketTrends trends,
            AdCreative creative
    ) {
        return new MarketReport(
                UUID.randomUUID().toString(),
                Instant.now(),
                product,
                gtmPlan,
                trends,
                creative
        );
    }
}
