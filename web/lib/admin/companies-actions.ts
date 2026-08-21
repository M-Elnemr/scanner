"use server";

import { revalidatePath } from "next/cache";
import { apiClient } from "@/lib/auth/server";
import { unwrap, unwrapVoid } from "@/lib/api/client";
import { ApiError } from "@/lib/api/errors";
import type { ActionResult } from "@/lib/leads/actions";

export interface CompanyAddressInput {
  cityId: string;
  addressText: string;
  mobileNumber: string;
}

export interface CompanyProfileInput {
  companyName: string;
  licenseNumber: string;
  whatsapp?: string;
  description?: string;
  addresses: CompanyAddressInput[];
}

function revalidateCompanies(id?: string) {
  revalidatePath("/admin/companies");
  if (id) revalidatePath(`/admin/companies/${id}`);
}

export async function createCompanyAction(
  input: CompanyProfileInput & { ownerEmail: string; commissionPerTraveler?: number; autoApprove: boolean },
): Promise<ActionResult<{ id: string }>> {
  try {
    const api = await apiClient();
    const result = await api.POST("/api/v1/admin/companies", { body: input });
    const company = unwrap<{ id?: string }>(result);
    revalidateCompanies();
    return { ok: true, data: { id: company?.id ?? "" } };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message, fieldErrors: error.fieldErrors };
    return { ok: false, error: "Could not create company." };
  }
}

export async function updateCompanyAction(id: string, input: CompanyProfileInput): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PUT("/api/v1/admin/companies/{id}", { params: { path: { id } }, body: input });
    unwrapVoid(result);
    revalidateCompanies(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message, fieldErrors: error.fieldErrors };
    return { ok: false, error: "Could not update company." };
  }
}

export async function deleteCompanyAction(id: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.DELETE("/api/v1/admin/companies/{id}", { params: { path: { id } } });
    unwrapVoid(result);
    revalidateCompanies();
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not delete company — it may have live bookings or an unsettled commission." };
  }
}

export async function approveCompanyAction(id: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/admin/companies/{id}/approve", { params: { path: { id } } });
    unwrapVoid(result);
    revalidateCompanies(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not approve company." };
  }
}

export async function rejectCompanyAction(id: string, reason: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/admin/companies/{id}/reject", { params: { path: { id } }, body: { reason } });
    unwrapVoid(result);
    revalidateCompanies(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not reject company." };
  }
}

export async function suspendCompanyAction(id: string, reason: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/admin/companies/{id}/suspend", { params: { path: { id } }, body: { reason } });
    unwrapVoid(result);
    revalidateCompanies(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not suspend company." };
  }
}

export async function reinstateCompanyAction(id: string): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/admin/companies/{id}/reinstate", { params: { path: { id } } });
    unwrapVoid(result);
    revalidateCompanies(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message };
    return { ok: false, error: "Could not reinstate company." };
  }
}

export async function setCommissionAction(id: string, commissionPerTraveler: number): Promise<ActionResult> {
  try {
    const api = await apiClient();
    const result = await api.PATCH("/api/v1/admin/companies/{id}/commission", {
      params: { path: { id } },
      body: { commissionPerTraveler },
    });
    unwrapVoid(result);
    revalidateCompanies(id);
    return { ok: true };
  } catch (error) {
    if (error instanceof ApiError) return { ok: false, error: error.message, fieldErrors: error.fieldErrors };
    return { ok: false, error: "Could not update commission." };
  }
}
