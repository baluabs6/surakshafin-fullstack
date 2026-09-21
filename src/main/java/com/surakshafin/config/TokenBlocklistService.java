package com.surakshafin.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
