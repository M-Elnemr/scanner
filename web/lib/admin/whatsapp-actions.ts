"use server";

import { apiClient } from "@/lib/auth/server";
import { unwrap } from "@/lib/api/client";
import { ApiError } from "@/lib/api/errors";

export interface WhatsAppMessage {
  recipientName?: string;
  recipientPhone?: string;
  message?: string;
  link?: string;
}

export async function composeTripWhatsAppAction(
  leadId: string,
  lang: "ar" | "en" = "ar",
): Promise<{ ok: true; data: WhatsAppMessage } | { ok: false; error: string }> {
  try {
    const api = await apiClient();
    const result = await api.GET("/api/v1/admin/leads/{id}/whatsapp/trip", {
      params: { path: { id: leadId }, query: { lang } },
      cache: "no-store",
    });
    return { ok: true, data: unwrap<WhatsAppMessage>(result) };
  } catch (error) {
    return { ok: false, error: error instanceof ApiError ? error.message : "Could not compose the message." };
  }
}

export async function composeCompanyWhatsAppAction(
  leadId: string,
  lang: "ar" | "en" = "ar",
): Promise<{ ok: true; data: WhatsAppMessage } | { ok: false; error: string }> {
  try {
    const api = await apiClient();
    const result = await api.GET("/api/v1/admin/leads/{id}/whatsapp/company", {
      params: { path: { id: leadId }, query: { lang } },
      cache: "no-store",
    });
    return { ok: true, data: unwrap<WhatsAppMessage>(result) };
  } catch (error) {
    return { ok: false, error: error instanceof ApiError ? error.message : "Could not compose the message." };
  }
}
