package com.market360.core.model;

import java.util.List;

/**
 * Copy publicitaire et brief visuels produits par le CreativeAgent.
 */
public record AdCreative(
        String headline,
        String subheadline,
        String cta,
        List<String> adVariants,
        String visualBrief,
        String imageUrl         // URL fal.ai (nullable → Optional en service layer)
) {
    public AdCreative {
        adVariants = adVariants == null ? List.of() : List.copyOf(adVariants);
    }
}
