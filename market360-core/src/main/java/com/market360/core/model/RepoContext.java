package com.market360.core.model;

import java.util.List;

/**
 * Données brutes récupérées depuis l'API GitHub.
 * Transmises à l'AnalystAgent comme contexte pour Claude.
 */
public record RepoContext(
        String owner,
        String repo,
        String repoUrl,
        String name,
        String description,
        String defaultBranch,
        int stars,
        int openIssues,
        int forks,
        String primaryLanguage,
        List<String> topics,
        String readmeContent     // contenu brut du README (markdown)
) {
    public RepoContext {
        topics       = topics == null ? List.of() : List.copyOf(topics);
        readmeContent = readmeContent == null ? "" : readmeContent;
        description   = description   == null ? "" : description;
    }

    /** Résumé compact pour logs */
    public String summary() {
        return "%s/%s (%d stars, %d issues, %s)".formatted(owner, repo, stars, openIssues, primaryLanguage);
    }
}
