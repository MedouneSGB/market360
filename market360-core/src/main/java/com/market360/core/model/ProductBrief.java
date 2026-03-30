package com.market360.core.model;

import java.util.List;

/**
 * Résultat de l'analyse d'un repo GitHub par l'AnalystAgent.
 * Record immuable — jamais de setters.
 */
public record ProductBrief(
        String name,
        String description,
        String stack,
        String audience,
        String usp,
        List<String> keyFeatures,
        String repositoryUrl,
        int githubStars,
        int openIssues
) {
    /** Constructeur compact de validation */
    public ProductBrief {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (repositoryUrl == null || repositoryUrl.isBlank()) throw new IllegalArgumentException("repositoryUrl is required");
        keyFeatures = keyFeatures == null ? List.of() : List.copyOf(keyFeatures);
    }
}
