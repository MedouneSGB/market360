package com.market360.agents;

import com.market360.agents.github.GitHubClient;
import com.market360.core.model.ProductBrief;
import com.market360.core.model.RepoContext;
import com.market360.skills.SkillLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Analyse un repo GitHub et retourne un ProductBrief structuré.
 *
 * Flux :
 *   1. GitHubClient  → RepoContext (API GitHub réelle : metadata + README)
 *   2. ChatClient    → ProductBrief (Claude analyse les données réelles)
 *
 * Modèle : claude-sonnet-4-6
 */
@Service
public class AnalystAgent {

    private static final Logger log = LoggerFactory.getLogger(AnalystAgent.class);

    private final GitHubClient gitHubClient;
    private final ChatClient   chatClient;

    public AnalystAgent(GitHubClient gitHubClient, ChatClient.Builder builder) {
        this.gitHubClient = gitHubClient;
        this.chatClient   = builder
                .defaultSystem(SkillLoader.load("repo-analysis"))
                .build();
    }

    public ProductBrief analyze(String repoUrl) {
        log.debug("Fetching GitHub context for {}", repoUrl);
        RepoContext ctx = gitHubClient.fetchRepoContext(repoUrl);
        log.info("GitHub context fetched: {}", ctx.summary());

        return chatClient.prompt()
                .user(u -> u.text(buildPrompt(ctx)))
                .call()
                .entity(ProductBrief.class);
    }

    // --- Privé ---

    private String buildPrompt(RepoContext ctx) {
        // Tronquer le README à 8000 chars pour rester dans la fenêtre de contexte
        String readme = ctx.readmeContent().length() > 8_000
                ? ctx.readmeContent().substring(0, 8_000) + "\n\n[README tronqué]"
                : ctx.readmeContent();

        return """
                ## Repository Metadata

                - URL         : %s
                - Name        : %s
                - Description : %s
                - Language    : %s
                - Topics      : %s
                - Stars       : %d
                - Open issues : %d
                - Forks       : %d

                ## README Content

                %s
                """.formatted(
                ctx.repoUrl(),
                ctx.name(),
                ctx.description(),
                ctx.primaryLanguage(),
                String.join(", ", ctx.topics()),
                ctx.stars(),
                ctx.openIssues(),
                ctx.forks(),
                readme
        );
    }
}
