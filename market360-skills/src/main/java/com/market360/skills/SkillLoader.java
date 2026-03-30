package com.market360.skills;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Charge un skill Markdown depuis le classpath.
 *
 * Les skills se trouvent dans {@code resources/skills/<name>.md}.
 * Utilisé par les agents pour construire leur system prompt.
 */
public class SkillLoader {

    private SkillLoader() {}

    /**
     * Charge le contenu Markdown d'un skill.
     *
     * @param skillName nom du skill sans extension (ex: "repo-analysis")
     * @return contenu du fichier Markdown
     * @throws UncheckedIOException si le skill n'existe pas
     */
    public static String load(String skillName) {
        var resource = new ClassPathResource("skills/" + skillName + ".md");
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Skill introuvable : skills/" + skillName + ".md", e);
        }
    }
}
