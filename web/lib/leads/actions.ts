"use server";

import { revalidatePath } from "next/cache";
import { apiClient } from "@/lib/auth/server";
import { unwrapVoid } from "@/lib/api/client";
import { ApiError } from "@/lib/api/errors";

export interface ActionResult<T = undefined> {
  ok: boolean;
  data?: T;
  error?: string;
  code?: string;
  activeLead?: { leadId: string; tripId: string; tripTitle: string; status: string };
  fieldErrors?: Record<string, string>;
}

function fail(error: unknown): ActionResult<never> {
  if (error instanceof ApiError) {
    return {
      ok: false,
      error: error.message,
      code: error.code,
      activeLead: error.problem.activeLead,
      fieldErrors: error.fieldErrors,
    };
  }
  return { ok: false, error: "Something went wrong. Please try again." };
}

/** The traveler-picker submit — creates or resumes the lead on this trip. */
export async function preserveTripAction(
  tripId: string,
  adultCount: number,
  childCount: number,
  infantCount: number,
): Promise<ActionResult<{ leadId: string }>> {
  try {
    const api = await apiClient();
    const result = await api.POST("/api/v1/trips/{tripId}/contact-company", {
      params: { path: { tripId } },
      body: { adultCount, childCount, infantCount },
    });
    unwrapVoid(result);
    const lead = result.data?.data;
    if (!lead?.id) return { ok: false, error: "Could not preserve this journey." };
    revalidatePath(`/trips/${tripId}`);
    revalidatePath(`/leads/${lead.id}`);
    return { ok: true, data: { leadId: lead.id } };
  } catch (error) {
    return fail(error);
  }
}

export async function cancelLeadAction(leadId: string, reason: string | undefined, tripId?: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/customers/me/leads/{id}/cancel", {
      params: { path: { id: leadId } },
      body: reason ? { note: reason } : {},
    });
    unwrapVoid(result);
    revalidatePath(`/leads/${leadId}`);
    revalidatePath("/leads");
    if (tripId) revalidatePath(`/trips/${tripId}`);
    return { ok: true };
  } catch (error) {
    return fail(error);
  }
}

export async function updateTravelersAction(
  leadId: string,
  adultCount: number,
  childCount: number,
  infantCount: number,
): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PUT("/api/v1/customers/me/leads/{id}/travelers", {
      params: { path: { id: leadId } },
      body: { adultCount, childCount, infantCount },
    });
    unwrapVoid(result);
    revalidatePath(`/leads/${leadId}`);
    return { ok: true };
  } catch (error) {
    return fail(error);
  }
}

export async function submitRatingAction(leadId: string, stars: number, comment?: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.POST("/api/v1/leads/{id}/rating", {
      params: { path: { id: leadId } },
      body: { stars, comment },
    });
    unwrapVoid(result);
    revalidatePath(`/leads/${leadId}`);
    return { ok: true };
  } catch (error) {
    return fail(error);
  }
}

export async function toggleFavouriteAction(tripId: string, currentlyFavourited: boolean): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = currentlyFavourited
      ? await api.DELETE("/api/v1/customers/me/favourites/{tripId}", { params: { path: { tripId } } })
      : await api.POST("/api/v1/customers/me/favourites/{tripId}", { params: { path: { tripId } } });
    unwrapVoid(result);
    revalidatePath(`/trips/${tripId}`);
    revalidatePath("/favourites");
    return { ok: true };
  } catch (error) {
    return fail(error);
  }
}
