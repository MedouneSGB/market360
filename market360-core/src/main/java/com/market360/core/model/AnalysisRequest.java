package com.market360.core.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Requête d'analyse reçue via l'API REST.
 */
public record AnalysisRequest(
        @NotBlank String repoUrl,
        @Pattern(regexp = "local|global") String market,
        String location     // ex: "Dakar", "Paris" — facultatif si market=global
) {
    public AnalysisRequest {
        market = (market == null || market.isBlank()) ? "global" : market;
    }
}
