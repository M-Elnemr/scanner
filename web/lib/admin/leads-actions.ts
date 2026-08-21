"use server";

import { revalidatePath } from "next/cache";
import { apiClient } from "@/lib/auth/server";
import { unwrapVoid } from "@/lib/api/client";
import { ApiError } from "@/lib/api/errors";
import type { ActionResult } from "@/lib/leads/actions";

type LeadStatus =
  | "INTERESTED"
  | "CONTACTED"
  | "CONFIRMED"
  | "PENDING_DEPOSIT_CONFIRMATION"
  | "DEPOSIT_PAID"
  | "PENDING_FULL_PAYMENT_CONFIRMATION"
  | "FULLY_PAID"
  | "PENDING_COMMISSION_CONFIRMATION"
  | "COMMISSION_PAID"
  | "CASHBACK_PAID"
  | "CANCELLED";

function revalidateLead(id: string) {
  revalidatePath("/admin/leads");
  revalidatePath(`/admin/leads/${id}`);
}

export async function overrideLeadStatusAction(id: string, status: LeadStatus, reason: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/admin/leads/{id}/status", { params: { path: { id } }, body: { status, reason } });
    unwrapVoid(result);
    revalidateLead(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not override this lead's status." };
  }
}

export async function markContactedAction(id: string, note?: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/admin/leads/{id}/mark-contacted", {
      params: { path: { id } },
      body: note ? { note } : undefined,
    });
    unwrapVoid(result);
    revalidateLead(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not mark this lead as contacted." };
  }
}

export async function confirmViaCompanyAction(id: string, note?: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/admin/leads/{id}/mark-confirmed", {
      params: { path: { id } },
      body: note ? { note } : undefined,
    });
    unwrapVoid(result);
    revalidateLead(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not mark this lead as confirmed." };
  }
}

export async function confirmCommissionAction(id: string, note?: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/admin/leads/{id}/confirm-commission", {
      params: { path: { id } },
      body: note ? { note } : undefined,
    });
    unwrapVoid(result);
    revalidateLead(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not confirm commission." };
  }
}

export async function payCashbackAction(id: string, note?: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/admin/leads/{id}/pay-cashback", {
      params: { path: { id } },
      body: note ? { note } : undefined,
    });
    unwrapVoid(result);
    revalidateLead(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not record the cashback payout." };
  }
}
