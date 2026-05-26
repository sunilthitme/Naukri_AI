package com.naukri.bot.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class BotRuntimeState {
    private final ConcurrentHashMap<Long, AtomicBoolean> stopFlags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicBoolean> pauseFlags = new ConcurrentHashMap<>();

    public void start(Long userId) {
        stopFlags.computeIfAbsent(userId, ignored -> new AtomicBoolean()).set(false);
        pauseFlags.computeIfAbsent(userId, ignored -> new AtomicBoolean()).set(false);
    }

    public void stop(Long userId) {
        stopFlags.computeIfAbsent(userId, ignored -> new AtomicBoolean()).set(true);
        pauseFlags.computeIfAbsent(userId, ignored -> new AtomicBoolean()).set(false);
    }

    public void pause(Long userId) {
        pauseFlags.computeIfAbsent(userId, ignored -> new AtomicBoolean()).set(true);
    }

    public void resume(Long userId) {
        pauseFlags.computeIfAbsent(userId, ignored -> new AtomicBoolean()).set(false);
    }

    public boolean shouldStop(Long userId) {
        return stopFlags.getOrDefault(userId, new AtomicBoolean(false)).get();
    }

    public boolean isPaused(Long userId) {
        return pauseFlags.getOrDefault(userId, new AtomicBoolean(false)).get();
    }
}
