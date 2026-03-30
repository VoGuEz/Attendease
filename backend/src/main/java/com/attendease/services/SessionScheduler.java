package com.attendease.services;

import com.attendease.repositories.SessionRepository;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionScheduler {
    private final SessionRepository sessionRepository;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SessionScheduler(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Starts a background task that checks every minute for active sessions
     * whose end time has passed and marks them as completed automatically.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int ended = sessionRepository.endExpiredSessions();
                if (ended > 0) {
                    System.out.println("[SessionScheduler] Auto-ended " + ended + " expired session(s).");
                }
            } catch (SQLException e) {
                System.err.println("[SessionScheduler] Error checking expired sessions: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);

        System.out.println("[SessionScheduler] Started — checking for expired sessions every minute.");
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}