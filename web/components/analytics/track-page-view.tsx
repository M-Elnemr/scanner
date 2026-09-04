"use client";

import { useEffect } from "react";
import { trackEvent } from "@/lib/analytics/track";

/**
 * Renders nothing — fires one `trackEvent` call per mount (i.e. per navigation to the page it's
 * placed on). Lets a Server Component page track a view without itself needing to be a Client
 * Component; `metadata` is only read on mount, not re-fired if its identity changes on a re-render.
 */
export function TrackPageView({
  eventType,
  entityType,
  entityId,
  metadata,
}: {
  eventType: string;
  entityType?: string;
  entityId?: string;
  metadata?: Record<string, unknown>;
}) {
  useEffect(() => {
    trackEvent(eventType, { entityType, entityId, metadata });
    // Fires once per mount/navigation — deliberately not re-run on every metadata identity change.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventType, entityType, entityId]);

  return null;
}
