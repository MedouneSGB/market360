package com.market360.core.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Requête d'analyse reçue via l'API REST.
 *
 * repoUrl doit être une URL GitHub valide (https://github.com/owner/repo).
 * Pas de chemins supplémentaires ni de query strings pour éviter les injections.
 */
public record AnalysisRequest(
        @NotBlank
        @Pattern(
            regexp = "https://github\\.com/[a-zA-Z0-9._-]+/[a-zA-Z0-9._-]+",
            message = "repoUrl must be a valid GitHub repository URL (https://github.com/owner/repo)"
        )
        String repoUrl,

        @Pattern(regexp = "local|global", message = "market must be 'local' or 'global'")
        String market,

        String location,    // ex: "Dakar", "Paris" — facultatif si market=global

        @Pattern(regexp = "fr|en", message = "language must be 'fr' or 'en'")
        String language     // langue du rapport — défaut "fr"
) {
    public AnalysisRequest {
        market   = (market   == null || market.isBlank())   ? "global" : market;
        language = (language == null || language.isBlank()) ? "fr"     : language;
    }

    /** Nom complet de la langue pour les instructions LLM. */
    public String languageName() {
        return "en".equals(language) ? "English" : "French";
    }
}
