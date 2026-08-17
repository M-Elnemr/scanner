"use server";

import { revalidatePath } from "next/cache";
import { apiClient } from "@/lib/auth/server";
import { unwrap, unwrapVoid } from "@/lib/api/client";
import { ApiError } from "@/lib/api/errors";
import type { ActionResult } from "@/lib/leads/actions";

export interface HotelInput {
  city: "MAKKAH" | "MADINAH";
  name: string;
  nameAr?: string;
  stars?: number;
  distanceToHaramM?: number;
  canWalk?: boolean;
  locationUrl?: string;
  active?: boolean;
}

export async function createHotelAction(input: HotelInput): Promise<ActionResult<{ id: string }>> {
  try {
    const api = await apiClient();
    const result = await api.POST("/api/v1/admin/hotels", { body: input });
    const hotel = unwrap<{ id?: string }>(result);
    revalidatePath("/admin/hotels");
    return { ok: true, data: { id: hotel?.id ?? "" } };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message, fieldErrors: error.fieldErrors };
    return { ok: false, error: "Could not create hotel." };
  }
}

export async function updateHotelAction(id: string, input: HotelInput): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PUT("/api/v1/admin/hotels/{id}", { params: { path: { id } }, body: input });
    unwrapVoid(result);
    revalidatePath("/admin/hotels");
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message, fieldErrors: error.fieldErrors };
    return { ok: false, error: "Could not update hotel." };
  }
}

export async function deleteHotelAction(id: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.DELETE("/api/v1/admin/hotels/{id}", { params: { path: { id } } });
    unwrapVoid(result);
    revalidatePath("/admin/hotels");
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not delete hotel — it may still be used by a trip. Mark it inactive instead." };
  }
}
