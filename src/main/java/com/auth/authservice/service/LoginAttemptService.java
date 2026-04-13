package com.auth.authservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_SECONDS = 600; // 10 minutes

    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<String, Instant> blockedUntil = new ConcurrentHashMap<>();

    public boolean isBlocked(String email) {
        Instant blockTime = blockedUntil.get(email);
        if (blockTime == null) return false;
        if (Instant.now().isAfter(blockTime)) {
            blockedUntil.remove(email);
            attempts.remove(email);
            return false;
        }
        return true;
    }

    public void loginFailed(String email) {
        int count = attempts.merge(email, 1, Integer::sum);
        if (count >= MAX_ATTEMPTS) {
            blockedUntil.put(email, Instant.now().plusSeconds(BLOCK_DURATION_SECONDS));
            log.warn("Account temporarily blocked due to too many failed attempts: {}", email);
        }
    }

    public void loginSucceeded(String email) {
        attempts.remove(email);
        blockedUntil.remove(email);
    }
}
