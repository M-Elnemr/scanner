package com.umrah.scanner.analytics.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A minimal per-IP fixed-window limiter for the one endpoint reachable with no authentication at
 * all: {@code POST /api/v1/analytics/events} (opened up to guests — see
 * {@code AnalyticsEventController}). In-process/in-memory only, which is fine for this app's
 * current single-VPS deployment; a multi-instance deployment would need a shared store (e.g.
 * Redis) instead. No new dependency was pulled in for this — every other rate-limiting concern in
 * the app is upstream of JWT auth, so a library for this one anonymous-only path wasn't worth it.
 */
public class AnalyticsRateLimitFilter extends OncePerRequestFilter {

    private static final String PATH = "/api/v1/analytics/events";
    private static final int MAX_REQUESTS_PER_WINDOW = 60;
    private static final long WINDOW_MILLIS = 60_000;
    // Stale windows are swept periodically rather than on every request, keyed off this counter,
    // so the map can't grow unbounded from a stream of distinct IPs without adding per-request cost.
    private static final long SWEEP_EVERY_N_REQUESTS = 500;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !PATH.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        Window window = windows.computeIfAbsent(clientIp(request), ip -> new Window());
        if (window.tryConsume()) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429); // Too Many Requests — not in HttpServletResponse's constant set
        }

        if (requestCount.incrementAndGet() % SWEEP_EVERY_N_REQUESTS == 0) {
            long now = System.currentTimeMillis();
            windows.values().removeIf(w -> w.isStale(now));
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        private long windowStart = System.currentTimeMillis();
        private int count;

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= WINDOW_MILLIS) {
                windowStart = now;
                count = 0;
            }
            return ++count <= MAX_REQUESTS_PER_WINDOW;
        }

        synchronized boolean isStale(long now) {
            return now - windowStart >= WINDOW_MILLIS * 5;
        }
    }
}
