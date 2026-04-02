package com.market360.api.ratelimit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting par IP — 3 analyses par jour.
 * Reset automatique chaque jour à minuit UTC.
 * Stockage en mémoire (suffisant pour MVP).
 */
@Service
public class RateLimitService {

    static final int DAILY_LIMIT = 3;

    private final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    /**
     * Tente de consommer un crédit pour l'IP donnée.
     * @return true si autorisé, false si limite atteinte
     */
    public boolean tryConsume(String ip) {
        var counter = counts.computeIfAbsent(ip, _ -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        return current <= DAILY_LIMIT;
    }

    /** Crédits restants pour l'IP (0 si épuisé). */
    public int remaining(String ip) {
        var counter = counts.get(ip);
        if (counter == null) return DAILY_LIMIT;
        return Math.max(0, DAILY_LIMIT - counter.get());
    }

    /** Reset quotidien à minuit UTC. */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void resetDaily() {
        counts.clear();
    }
}
