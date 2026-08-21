"use server";

import { revalidatePath } from "next/cache";
import { apiClient } from "@/lib/auth/server";
import { unwrapVoid } from "@/lib/api/client";
import { ApiError } from "@/lib/api/errors";
import type { ActionResult } from "@/lib/leads/actions";

export async function markNotificationReadAction(id: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/notifications/{id}/read", { params: { path: { id } } });
    unwrapVoid(result);
    revalidatePath("/notifications");
    return { ok: true };
  } catch {
    return { ok: false, error: "Could not update this notification." };
  }
}

export async function updateProfileAction(input: { fullName: string; phone: string }): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PUT("/api/v1/customers/me", { body: input });
    unwrapVoid(result);
    revalidatePath("/profile");
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) {
      return { ok: false, error: error.message, fieldErrors: error.fieldErrors };
    }
    return { ok: false, error: "Could not update your profile." };
  }
}
