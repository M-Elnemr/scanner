"use client";

const ENDPOINT = "/api/analytics/events";

/**
 * Fire-and-forget usage tracking for the public site — guests and signed-in users alike (see
 * app/api/analytics/events/route.ts, which proxies to the backend's analytics_events pipeline).
 * Never throws and never blocks rendering: `navigator.sendBeacon` when available (survives page
 * unload, which a plain fetch wouldn't for a call fired just before navigation), falling back to
 * `fetch(..., { keepalive: true })` on the rare browser without it.
 */
export function trackEvent(
  eventType: string,
  opts?: { entityType?: string; entityId?: string; metadata?: Record<string, unknown> },
) {
  try {
    const payload = JSON.stringify({
      eventType,
      entityType: opts?.entityType,
      entityId: opts?.entityId,
      metadata: opts?.metadata,
    });

    if (typeof navigator !== "undefined" && navigator.sendBeacon) {
      navigator.sendBeacon(ENDPOINT, new Blob([payload], { type: "application/json" }));
      return;
    }

    void fetch(ENDPOINT, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: payload,
      keepalive: true,
    }).catch(() => undefined);
  } catch {
    // Tracking must never break the page it's called from.
  }
}
