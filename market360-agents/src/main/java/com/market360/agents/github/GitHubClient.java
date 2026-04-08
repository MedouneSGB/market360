package com.market360.agents.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market360.core.model.RepoContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Client GitHub REST API v3.
 *
 * Récupère les métadonnées du repo + contenu du README et les packge
 * dans un {@link RepoContext} prêt à être consommé par l'AnalystAgent.
 *
 * Authentification : GITHUB_TOKEN optionnel (60 req/h sans, 5000 req/h avec).
 */
@Service
public class GitHubClient {

    private static final String API_BASE    = "https://api.github.com";
    private static final String USER_AGENT  = "Market360-AnalystAgent/0.1";
    private static final Pattern REPO_PATTERN =
            Pattern.compile("https://github\\.com/([a-zA-Z0-9._-]+)/([a-zA-Z0-9._-]+)");

    private final HttpClient   httpClient;
    private final ObjectMapper mapper;
    private final String       token;

    public GitHubClient(
            ObjectMapper objectMapper,
            @Value("${github.token:}") String token
    ) {
        this.mapper     = objectMapper;
        this.token      = token;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .executor(Runnable::run)          // délègue aux virtual threads appelants
                .build();
    }

    /**
     * Récupère le contexte complet d'un repo GitHub.
     *
     * @param repoUrl URL GitHub validée (https://github.com/owner/repo)
     * @return RepoContext avec métadonnées + README
     * @throws GitHubApiException si le repo est introuvable ou l'API renvoie une erreur
     */
    public RepoContext fetchRepoContext(String repoUrl) {
        var matcher = REPO_PATTERN.matcher(repoUrl);
        if (!matcher.matches()) {
            throw new GitHubApiException("URL GitHub invalide : " + repoUrl);
        }
        String owner = matcher.group(1);
        String repo  = matcher.group(2);

        JsonNode repoNode   = get("/repos/%s/%s".formatted(owner, repo));
        String   readmeText = fetchReadme(owner, repo);

        return new RepoContext(
                owner,
                repo,
                repoUrl,
                repoNode.path("name").asText(repo),
                repoNode.path("description").asText(""),
                repoNode.path("default_branch").asText("main"),
                repoNode.path("stargazers_count").asInt(0),
                repoNode.path("open_issues_count").asInt(0),
                repoNode.path("forks_count").asInt(0),
                repoNode.path("language").asText(""),
                extractTopics(repoNode),
                readmeText
        );
    }

    // --- Privé ---

    private JsonNode get(String path) {
        var request = baseRequest(API_BASE + path).GET().build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 404) {
                throw new GitHubApiException(
                        "Repo introuvable — vérifiez l'URL (le dépôt n'existe pas ou a été supprimé) : " + path);
            }
            if (status == 401 || status == 403) {
                throw new GitHubApiException(
                        "Accès refusé — ce dépôt est privé ou nécessite une authentification (HTTP " + status + ") : " + path);
            }
            if (status != 200) {
                throw new GitHubApiException(
                        "GitHub API erreur %d : %s".formatted(status, path));
            }
            return mapper.readTree(response.body());
        } catch (GitHubApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GitHubApiException("Erreur appel GitHub API : " + path, e);
        }
    }

    /** Retourne le contenu décodé du README, ou chaîne vide si absent. */
    private String fetchReadme(String owner, String repo) {
        var request = baseRequest(API_BASE + "/repos/%s/%s/readme".formatted(owner, repo))
                .GET().build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return "";
            JsonNode node    = mapper.readTree(response.body());
            String   encoded = node.path("content").asText("");
            // L'API GitHub encode le README en base64 avec des sauts de ligne
            String cleaned = encoded.replaceAll("\\s", "");
            return new String(Base64.getDecoder().decode(cleaned));
        } catch (Exception e) {
            return "";   // README absent ou non lisible — non bloquant
        }
    }

    private List<String> extractTopics(JsonNode repoNode) {
        var topics = new ArrayList<String>();
        var topicsNode = repoNode.path("topics");
        if (topicsNode.isArray()) {
            topicsNode.forEach(t -> topics.add(t.asText()));
        }
        return topics;
    }

    private HttpRequest.Builder baseRequest(String url) {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", USER_AGENT)
                .header("X-GitHub-Api-Version", "2022-11-28");
        if (!token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }
}
