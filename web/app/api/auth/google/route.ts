import { NextResponse } from "next/server";
import { publicApi, unwrap } from "@/lib/api/client";
import { ApiError } from "@/lib/api/errors";
import { getSession } from "@/lib/auth/session";

/**
 * Receives the Google ID token the client-side sign-in widget already obtained, exchanges it with
 * the backend, and stores the resulting token pair in the httpOnly session cookie — the raw tokens
 * never reach browser JS from here on.
 *
 * `role: "CUSTOMER"` is a hint only consulted on a brand-new Google subject (see LoginRequest's own
 * javadoc). This site never lets anyone self-register as ADMIN or COMPANY — an admin's account
 * already exists (seeded directly in the database) and the hint is ignored for them, so hardcoding
 * CUSTOMER here is correct for every caller, not just customers.
 */
export async function POST(request: Request) {
  const body = (await request.json().catch(() => null)) as { idToken?: string } | null;
  if (!body?.idToken) {
    return NextResponse.json({ detail: "idToken is required" }, { status: 400 });
  }

  try {
    const result = await publicApi.POST("/api/v1/auth/google", {
      body: { idToken: body.idToken, role: "CUSTOMER" },
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
