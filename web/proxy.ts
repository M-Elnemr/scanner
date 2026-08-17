import { NextResponse, type NextRequest } from "next/server";
import { sealData } from "iron-session";
import { readSessionFromCookieValue, getSessionOptions, SESSION_COOKIE_NAME, type SessionData } from "@/lib/auth/session";

const CLIENT_ONLY_PREFIXES = ["/leads", "/favourites", "/profile", "/notifications", "/trips/compare"];
const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  let session = await readSessionFromCookieValue(request.cookies.get(SESSION_COOKIE_NAME)?.value);

  // Proactively refresh here rather than in a Server Component render: proxy is the one place in
  // this app (besides Route Handlers/Server Actions) allowed to write cookies. Refreshing here
  // means a page render only ever needs to READ a token that is already fresh, instead of every
  // Server Component racing to refresh-and-write one — which Next.js forbids outright and would
  // otherwise crash the render (cookies() is read-only during a plain GET).
  let refreshedSetCookie: string | null = null;
  if (session?.accessToken && session.refreshToken) {
    const expiresAt = session.accessTokenExpiresAt ? new Date(session.accessTokenExpiresAt).getTime() : 0;
    if (expiresAt - Date.now() < 60_000) {
      const refreshed = await tryRefresh(session.refreshToken);
      if (refreshed) {
        session = { ...session, ...refreshed };
        refreshedSetCookie = await sealData(session, { password: getSessionOptions().password });
      } else {
        session = null;
      }
    }
  }

  const isAuthenticated = Boolean(session?.accessToken);
  const isAdmin = session?.role === "ADMIN";

  const response = routeFor(pathname, isAuthenticated, isAdmin, request);
  if (refreshedSetCookie) {
    response.cookies.set(SESSION_COOKIE_NAME, refreshedSetCookie, getSessionOptions().cookieOptions);
  }
  return response;
}

function routeFor(pathname: string, isAuthenticated: boolean, isAdmin: boolean, request: NextRequest) {
  if (pathname === "/login") {
    return isAuthenticated
      ? NextResponse.redirect(new URL(isAdmin ? "/admin" : "/", request.url))
      : NextResponse.next();
  }

  if (pathname.startsWith("/admin")) {
    if (!isAuthenticated) return redirectToLogin(request);
    if (!isAdmin) return NextResponse.redirect(new URL("/", request.url));
    return NextResponse.next();
  }

  if (CLIENT_ONLY_PREFIXES.some((prefix) => pathname.startsWith(prefix)) && !isAuthenticated) {
    return redirectToLogin(request);
  }

  return NextResponse.next();
}

async function tryRefresh(refreshToken: string): Promise<Pick<SessionData, "accessToken" | "accessTokenExpiresAt" | "refreshToken"> | null> {
  try {
    const res = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return null;
    const body = (await res.json()) as {
      data?: { accessToken?: string; accessTokenExpiresAt?: string; refreshToken?: string };
    };
    const tokens = body.data;
    if (!tokens?.accessToken || !tokens.accessTokenExpiresAt || !tokens.refreshToken) return null;
    return {
      accessToken: tokens.accessToken,
      accessTokenExpiresAt: tokens.accessTokenExpiresAt,
      refreshToken: tokens.refreshToken,
    };
  } catch {
    return null;
  }
}

function redirectToLogin(request: NextRequest) {
  const url = new URL("/login", request.url);
  url.searchParams.set("next", request.nextUrl.pathname + request.nextUrl.search);
  return NextResponse.redirect(url);
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|api/).*)"],
};
