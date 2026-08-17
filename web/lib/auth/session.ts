import { getIronSession, unsealData, type SessionOptions } from "iron-session";
import { cookies } from "next/headers";

/**
 * Held server-side in one httpOnly cookie — never in browser JS. The backend only speaks Bearer
 * tokens with no cookie support of its own, so something has to hold the pair; this is the "BFF"
 * layer that does it. See web/README (auth section) for the full rationale.
 */
export interface SessionData {
  userId?: string;
  role?: "CUSTOMER" | "COMPANY" | "ADMIN";
  accessToken?: string;
  /** ISO instant — checked before every server-side API call to decide whether to refresh first. */
  accessTokenExpiresAt?: string;
  refreshToken?: string;
}

export const SESSION_COOKIE_NAME = "us_session";

// A function rather than a module-level constant: `next build` imports every route module (and
// this middleware) to collect its metadata, which would run this before any real SESSION_SECRET
// is ever supplied to the build.
export function getSessionOptions(): SessionOptions {
  return {
    password: requireSecret(),
    cookieName: SESSION_COOKIE_NAME,
    cookieOptions: {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      // Matches the backend's refresh-token TTL (P30D) — once the refresh token itself expires,
      // there is nothing left to renew, so there is no point outliving it.
      maxAge: 60 * 60 * 24 * 30,
      path: "/",
    },
  };
}

function requireSecret(): string {
  const secret = process.env.SESSION_SECRET;
  if (!secret || secret.length < 32) {
    throw new Error("SESSION_SECRET must be set to a random string of at least 32 characters");
  }
  return secret;
}

/** Use inside Route Handlers and Server Actions — the only places a session can be written. */
export async function getSession() {
  return getIronSession<SessionData>(await cookies(), getSessionOptions());
}

/**
 * A read-only decode for contexts that don't have the mutable `next/headers` cookie store
 * (proxy.ts, which receives a `NextRequest` instead). Never writes.
 */
export async function readSessionFromCookieValue(value: string | undefined): Promise<SessionData | null> {
  if (!value) {
    return null;
  }
  try {
    return await unsealData<SessionData>(value, { password: requireSecret() });
  } catch {
    // Wrong/rotated secret, corrupted cookie, or expired seal — treat exactly like "logged out".
    return null;
  }
}

export function isAuthenticated(session: SessionData | null): session is SessionData & { accessToken: string } {
  return Boolean(session?.accessToken);
}
