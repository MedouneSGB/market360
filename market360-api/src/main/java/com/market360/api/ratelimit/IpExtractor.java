package com.market360.api.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extrait l'IP réelle du client derrière Cloud Run / un reverse proxy.
 * Cloud Run injecte l'IP dans X-Forwarded-For (premier élément = client réel).
 */
public final class IpExtractor {

    private IpExtractor() {}

    public static String extract(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // "client, proxy1, proxy2" → on veut "client"
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
