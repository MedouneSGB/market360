package com.market360.core.model;

import java.util.List;

/**
 * Plan Go-To-Market produit par le StrategistAgent.
 */
public record GTMPlan(
        String targetSegment,
        String positioningStatement,
        List<String> channels,
        List<String> milestones,
        String pricingStrategy,
        String competitiveAdvantage
) {
    public GTMPlan {
        channels  = channels  == null ? List.of() : List.copyOf(channels);
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
    }
}
