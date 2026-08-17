import { NextResponse } from "next/server";
import { publicApi, unwrap } from "@/lib/api/client";
import { ApiError } from "@/lib/api/errors";
import { getSession } from "@/lib/auth/session";

/**
 * Local-testing-only stand-in for the Google button, mirroring the backend's own
 * `POST /api/v1/auth/dev-google-test` (dev profile + app.dev-auth.enabled only — 404s otherwise).
 * Refuses to run outside development for the same reason the backend endpoint refuses outside its
 * own dev guard: this must never be reachable where a real sign-in is expected.
 */
export async function POST(request: Request) {
  if (process.env.NODE_ENV === "production") {
    return NextResponse.json({ detail: "Not found" }, { status: 404 });
  }

  const body = (await request.json().catch(() => null)) as { email?: string; role?: string } | null;
  if (!body?.email || !body.role) {
    return NextResponse.json({ detail: "email and role are required" }, { status: 400 });
  }

  try {
    const result = await publicApi.POST("/api/v1/auth/dev-google-test", {
      body: { email: body.email, role: body.role as "CUSTOMER" | "COMPANY" | "ADMIN" },
    });
    const tokens = unwrap(result);

    const session = await getSession();
    session.userId = tokens.userId;
    session.role = tokens.role;
    session.accessToken = tokens.accessToken;
    session.accessTokenExpiresAt = tokens.accessTokenExpiresAt;
    session.refreshToken = tokens.refreshToken;
    await session.save();

    return NextResponse.json({ role: tokens.role });
  } catch (error) {
    if (error instanceof ApiError) {
      return NextResponse.json({ detail: error.message }, { status: error.status });
    }
    return NextResponse.json({ detail: "Sign-in failed" }, { status: 500 });
  }
}
