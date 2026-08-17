"use server";

import { redirect } from "next/navigation";
import { publicApi } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";

export async function logoutAction(): Promise<void> {
  const session = await getSession();
  if (session.refreshToken) {
    // Best-effort: revoke the refresh token server-side so it can't be replayed. The session is
    // destroyed either way, even if the backend call fails (e.g. it was already expired).
    await publicApi
      .POST("/api/v1/auth/logout", { body: { refreshToken: session.refreshToken } })
      .catch(() => undefined);
  }
  session.destroy();
  redirect("/login");
}

export async function logoutAllDevicesAction(): Promise<void> {
  const { apiClient } = await import("@/lib/auth/server");
  const session = await getSession();
  const api = await apiClient();
  await api.POST("/api/v1/auth/logout-all").catch(() => undefined);
  session.destroy();
  redirect("/login");
}
