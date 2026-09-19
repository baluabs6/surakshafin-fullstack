package com.surakshafin.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security / feature gap fix: the original design had no logout endpoint at all — a stolen
 * bearer token stayed valid until it naturally expired, with no way for a user to revoke it.
 *
 * This is a minimal in-memory blocklist (fine for a single-instance demo deployment). In the
 * production architecture this is replaced by a shared store (e.g. Redis) keyed by token
 * fingerprint with a TTL equal to the token's remaining lifetime, so it scales across instances
 * and never grows unbounded.
 */
@Component
public class TokenBlocklistService {

    private final Map<String, Long> blockedUntilEpochMs = new ConcurrentHashMap<>();

    public void block(String token, long remainingValidityMillis) {
        cleanupExpired();
        blockedUntilEpochMs.put(token, System.currentTimeMillis() + remainingValidityMillis);
    }

    public boolean isBlocked(String token) {
        Long until = blockedUntilEpochMs.get(token);
        if (until == null) {
            return false;
        }
        if (until < System.currentTimeMillis()) {
            blockedUntilEpochMs.remove(token);
            return false;
        }
        return true;
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        blockedUntilEpochMs.entrySet().removeIf(e -> e.getValue() < now);
    }
}
