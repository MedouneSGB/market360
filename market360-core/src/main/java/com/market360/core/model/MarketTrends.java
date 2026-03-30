package com.market360.core.model;

import java.util.List;

/**
 * Tendances marché collectées par le TrendsAgent.
 */
public record MarketTrends(
        String location,
        String scope,           // "local" | "global"
        List<String> trends,
        List<String> competitors,
        List<String> opportunities,
        List<String> threats
) {
    public MarketTrends {
        trends       = trends       == null ? List.of() : List.copyOf(trends);
        competitors  = competitors  == null ? List.of() : List.copyOf(competitors);
        opportunities = opportunities == null ? List.of() : List.copyOf(opportunities);
        threats      = threats      == null ? List.of() : List.copyOf(threats);
    }
}
