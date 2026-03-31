package com.market360.agents.util;

import java.util.regex.Pattern;

/**
 * Extrait le JSON brut d'une réponse LLM qui peut contenir :
 * - Des blocs markdown ```json ... ```
 * - Du texte avant/après le JSON
 * - Des commentaires // ou #
 *
 * Utilisé pour normaliser les réponses des modèles locaux (Ollama)
 * qui ne respectent pas toujours le format "JSON uniquement".
 */
public class JsonSanitizer {

    private static final Pattern MD_FENCE  = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
    private static final Pattern JSON_OBJ  = Pattern.compile("\\{[\\s\\S]*\\}");
    private static final Pattern JSON_ARR  = Pattern.compile("\\[[\\s\\S]*\\]");

    private JsonSanitizer() {}

    /**
     * Extrait et retourne le premier objet JSON trouvé dans le texte.
     *
     * @param raw réponse brute du LLM
     * @return JSON nettoyé, ou le texte original si aucun JSON n'est trouvé
     */
    public static String extractJson(String raw) {
        if (raw == null || raw.isBlank()) return "{}";

        // 1. Si c'est déjà du JSON pur, retourner directement
        String trimmed = raw.strip();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }

        // 2. Extraire depuis un bloc ```json ... ```
        var mdMatcher = MD_FENCE.matcher(raw);
        if (mdMatcher.find()) {
            return mdMatcher.group(1).strip();
        }

        // 3. Chercher le premier objet JSON dans le texte
        var objMatcher = JSON_OBJ.matcher(raw);
        if (objMatcher.find()) {
            return objMatcher.group().strip();
        }

        // 4. Chercher un tableau JSON
        var arrMatcher = JSON_ARR.matcher(raw);
        if (arrMatcher.find()) {
            return arrMatcher.group().strip();
        }

        return raw;
    }
}
