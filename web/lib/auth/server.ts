import "server-only";
import { redirect } from "next/navigation";
import { createApiClient, publicApi, unwrap } from "@/lib/api/client";
import { getSession, isAuthenticated, type SessionData } from "@/lib/auth/session";

/**
 * Refreshes 60s before actual expiry (access tokens live 15 minutes) so a request never races the
 * clock. The backend rotates the refresh token on every use, so the new one must be persisted too —
 * skipping that would make the session die after exactly one refresh instead of lasting the full
 * 30-day refresh-token lifetime.
 *
 * <p>In the normal case proxy.ts (see proxy.ts's own comment) has already refreshed and rewritten
 * the cookie before a Server Component ever runs, so this rarely has to do the write itself here.
 * It can still be reached mid-render (a token that was fresh when proxy checked has since ticked
 * past the threshold), and `cookies()` is read-only during a plain page render — `session.save()`
 * throws there. The catch below is the safety net: use the refreshed token for *this* request's
 * API calls regardless, and let the next navigation (which proxy handles first) persist it.
 */
async function ensureFreshToken(session: Awaited<ReturnType<typeof getSession>>): Promise<string | undefined> {
  if (!isAuthenticated(session)) {
    return undefined;
  }
  const expiresAt = session.accessTokenExpiresAt ? new Date(session.accessTokenExpiresAt).getTime() : 0;
  if (expiresAt - Date.now() > 60_000) {
    return session.accessToken;
  }
  if (!session.refreshToken) {
    await tryDestroy(session);
    return undefined;
  }

  try {
    const result = await publicApi.POST("/api/v1/auth/refresh", {
      body: { refreshToken: session.refreshToken },
    });
    const tokens = unwrap(result);
    if (!tokens.accessToken || !tokens.accessTokenExpiresAt || !tokens.refreshToken) {
      await tryDestroy(session);
      return undefined;
    }
    session.accessToken = tokens.accessToken;
    session.accessTokenExpiresAt = tokens.accessTokenExpiresAt;
    session.refreshToken = tokens.refreshToken;
    await session.save().catch(() => undefined);
    return session.accessToken;
  } catch {
    // Refresh token expired/revoked — the session is dead either way.
    await tryDestroy(session);
    return undefined;
  }
}

async function tryDestroy(session: Awaited<ReturnType<typeof getSession>>) {
  try {
    session.destroy();
  } catch {
    // Read-only render context — nothing to do; proxy.ts will bounce the next navigation to /login.
  }
}

/**
 * The one function most server-side code should reach for: a fully authenticated (if logged in)
 * typed API client, transparently kept fresh. Safe to call from a logged-out context too — it just
 * behaves like the public client.
 */
export async function apiClient() {
  const session = await getSession();
  return createApiClient(() => ensureFreshToken(session));
}

export interface CurrentUser {
  userId: string;
  role: "CUSTOMER" | "COMPANY" | "ADMIN";
}

export async function getCurrentUser(): Promise<CurrentUser | null> {
  const session = await getSession();
  if (!isAuthenticated(session) || !session.userId || !session.role) {
    return null;
  }
  // Cheap liveness check: if the token can't be refreshed, treat as logged out.
  const token = await ensureFreshToken(session);
  if (!token) {
    return null;
  }
  return { userId: session.userId, role: session.role };
}

/** For Server Components/Actions that must not render at all without the right role. */
export async function requireUser(): Promise<CurrentUser> {
  const user = await getCurrentUser();
  if (!user) {
    redirect("/login");
  }
  return user;
}

export async function requireAdmin(): Promise<CurrentUser> {
  const user = await requireUser();
  if (user.role !== "ADMIN") {
    redirect("/");
  }
  return user;
}

export type { SessionData };
