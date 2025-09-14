package com.example.rabbit.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class InMemoryDedupService implements DedupService {
    // map to save all messages key - clear seen map every one hour to avoid out of memory
    private final Map<String, Long> seen = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public InMemoryDedupService(@Value("${dedup.ttl-seconds:3600}") long ttlSeconds) {
        this.ttlMillis = Duration.ofSeconds(ttlSeconds).toMillis();
    }

    @Override
    public boolean firstTimeSeen(String key) {
        long now = System.currentTimeMillis();
        // quick expiry sweep (amortized)
        /*This is a cheap cleanup strategy.

        Instead of running removeIf (which scans the whole map) on every single message,

        it only runs occasionally — once per 100 insertions.

        This way:

        Performance is good (no expensive cleanup for each message).

        Memory is kept under control (expired entries eventually get cleared).*/
        if (seen.size() % 100 == 0) {
            long cutoff = now - ttlMillis;
            seen.entrySet().removeIf(e -> e.getValue() < cutoff);
        }
        //try to save massage if true then first time otherwise it is already sent
        Long prev = seen.putIfAbsent(key, now);
        return prev == null; // null => first time we see this key
    }
}