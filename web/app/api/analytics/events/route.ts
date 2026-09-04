import { NextResponse } from "next/server";
import { apiClient } from "@/lib/auth/server";

/**
 * Thin proxy to the backend's `POST /api/v1/analytics/events`, kept in front of it for the same
 * reason every other backend call in this app goes through a Route Handler: browser JS never talks
 * to the backend directly (see lib/api/client.ts's own comment). `apiClient()` is "safe to call
 * from a logged-out context too — it just behaves like the public client" (its own doc comment),
 * so a guest's event reaches the backend with no Authorization header and a signed-in user's
 * carries their session-derived token — matching AnalyticsEventController's own guest/identified
 * split without this route needing to know which case it's in.
 *
 * Best-effort like the backend's own write path: never surfaces an error to the caller — a beacon
 * that fails shouldn't retry or throw in the page that fired it.
 */
export async function POST(request: Request) {
  const body = (await request.json().catch(() => null)) as {
    eventType?: string;
    entityType?: string;
    entityId?: string;
    metadata?: Record<string, unknown>;
  } | null;

  if (!body?.eventType) {
    return NextResponse.json({ detail: "eventType is required" }, { status: 400 });
  }

  try {
    const api = await apiClient();
    await api.POST("/api/v1/analytics/events", {
      body: {
        eventType: body.eventType,
        entityType: body.entityType,
        entityId: body.entityId,
        metadata: body.metadata,
      },
    });
  } catch {
    // Swallowed on purpose — see the module doc comment.
  }

  return new NextResponse(null, { status: 202 });
}
