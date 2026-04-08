package com.market360.agents.github;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du GitHubClient — validation du parsing d'URL uniquement.
 * Les appels réseau réels sont testés dans GitHubClientIT (Testcontainers/WireMock).
 */
class GitHubClientTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void urlValideAcceptee() {
        var client = new GitHubClient(mapper, "");
        // URL valide — ne doit pas lever d'exception lors du parsing
        // (l'appel réseau lèvera une exception, pas le parsing)
        assertThrows(GitHubApiException.class,
                () -> client.fetchRepoContext("https://github.com/spring-projects/spring-boot"),
                "Doit lever une exception réseau, pas de parsing");
    }

    @Test
    void urlInvalideRejetee() {
        var client = new GitHubClient(mapper, "");
        assertThrows(GitHubApiException.class,
                () -> client.fetchRepoContext("https://evil.com/inject"));
    }

    @Test
    void urlGitHubMaisCheminInvalideRejetee() {
        var client = new GitHubClient(mapper, "");
        assertThrows(GitHubApiException.class,
                () -> client.fetchRepoContext("https://github.com/"));
    }

    @Test
    void messageErreur404ContientInfosClaires() {
        var client = new GitHubClient(mapper, "");
        assertThatThrownBy(() -> client.fetchRepoContext("https://github.com/this-user-does-not-exist-xyz/nonexistent-repo-abc"))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void messageErreurUrlVideExplicite() {
        var client = new GitHubClient(mapper, "");
        assertThatThrownBy(() -> client.fetchRepoContext(""))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("invalide");
    }

    @Test
    void messageErreurUrlNonGitHub() {
        var client = new GitHubClient(mapper, "");
        assertThatThrownBy(() -> client.fetchRepoContext("https://gitlab.com/owner/repo"))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("invalide");
    }
}
