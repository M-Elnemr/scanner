"use server";

import { revalidatePath } from "next/cache";
import { apiClient } from "@/lib/auth/server";
import { unwrap, unwrapVoid } from "@/lib/api/client";
import { ApiError } from "@/lib/api/errors";
import type { ActionResult } from "@/lib/leads/actions";
import type { components } from "@/lib/api/schema";

export type TripDetail = components["schemas"]["TripDetail"];

export interface TripHotelInput {
  hotelId: string;
}

export interface RoomPriceInput {
  roomType: "SINGLE" | "DOUBLE" | "TRIPLE" | "QUAD" | "QUINT" | "CHILD" | "INFANT";
  price: number;
}

export interface TripInput {
  tripCode: string;
  title: string;
  departureDate: string;
  returnDate: string;
  outboundDepartureAirportId: string;
  outboundArrivalAirportId: string;
  returnDepartureAirportId: string;
  returnArrivalAirportId: string;
  airline: string;
  daysInMakkah?: number;
  daysInMadinah?: number;
  visaIncluded?: boolean;
  transportationIncluded?: boolean;
  mealsIncluded?: boolean;
  guideIncluded?: boolean;
  zamzamIncluded?: boolean;
  fastTrainIncluded?: boolean;
  description?: string;
  currencyId: string;
  availableSeats?: number;
  hotels?: TripHotelInput[];
  prices?: RoomPriceInput[];
  tier: "VIP" | "PREMIUM" | "ECONOMIC";
  commissionPerTraveler?: number;
}

function revalidateTrips(id?: string) {
  revalidatePath("/admin/trips");
  if (id) revalidatePath(`/admin/trips/${id}`);
}

export async function createTripAction(companyId: string, trip: TripInput): Promise<ActionResult<{ id: string }>> {
  try {
    const api = await apiClient();
    const result = await api.POST("/api/v1/admin/trips", { body: { companyId, trip } });
    const created = unwrap<{ id?: string }>(result);
    revalidateTrips();
    return { ok: true, data: { id: created?.id ?? "" } };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message, fieldErrors: error.fieldErrors };
    return { ok: false, error: "Could not create trip." };
  }
}

export async function updateTripAction(id: string, trip: TripInput): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PUT("/api/v1/admin/trips/{id}", { params: { path: { id } }, body: trip });
    unwrapVoid(result);
    revalidateTrips(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message, fieldErrors: error.fieldErrors };
    return { ok: false, error: "Could not update trip." };
  }
}

export async function deleteTripAction(id: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.DELETE("/api/v1/admin/trips/{id}", { params: { path: { id } } });
    unwrapVoid(result);
    revalidateTrips();
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not delete trip." };
  }
}

export async function changeTripStatusAction(
  id: string,
  status: "DRAFT" | "PUBLISHED" | "CLOSED" | "EXPIRED",
): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/admin/trips/{id}/status", { params: { path: { id } }, body: { status } });
    unwrapVoid(result);
    revalidateTrips(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not change trip status." };
  }
}

export async function getTripDetailsAction(id: string): Promise<ActionResult<TripDetail>> {
  try {
    const api = await apiClient();
    const result = await api.GET("/api/v1/admin/trips/{id}", { params: { path: { id } }, cache: "no-store" });
    return { ok: true, data: unwrap<TripDetail>(result) };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not load trip details." };
  }
}

export async function reassignTripCompanyAction(id: string, companyId: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/admin/trips/{id}/company", { params: { path: { id } }, body: { companyId } });
    unwrapVoid(result);
    revalidateTrips(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not reassign trip." };
  }
}
