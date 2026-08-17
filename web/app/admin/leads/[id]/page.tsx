import Link from "next/link";
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ArrowLeft, Users } from "lucide-react";
import { apiClient } from "@/lib/auth/server";
import { AdminLeadStatusBadge } from "@/components/admin/admin-lead-status-badge";
import { AdminLeadActions } from "@/components/admin/admin-lead-actions";
import { WhatsAppButtons } from "@/components/admin/whatsapp-buttons";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatDate } from "@/lib/format/date";
import { formatMoney } from "@/lib/format/money";

export const metadata: Metadata = { title: "Lead detail · Admin" };

export default async function AdminLeadDetailPage(props: PageProps<"/admin/leads/[id]">) {
  const { id } = await props.params;
  const api = await apiClient();
  const [detailResult, historyResult] = await Promise.all([
    api.GET("/api/v1/admin/leads/{id}", { params: { path: { id } }, cache: "no-store" }),
    api.GET("/api/v1/leads/{id}/history", { params: { path: { id } }, cache: "no-store" }),
  ]);
  const detail = detailResult.data?.data;
  const lead = detail?.lead;
  if (!lead?.id) notFound();
  const history = historyResult.data?.data ?? [];

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <Link href="/admin/leads" className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-3.5" /> Leads
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">{lead.tripTitle}</h1>
          <p className="text-sm text-muted-foreground">
            {lead.customer?.fullName} · {lead.customer?.phone}
          </p>
        </div>
        <AdminLeadStatusBadge status={lead.status} />
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="font-heading text-base">Travelers</CardTitle>
          </CardHeader>
          <CardContent className="flex items-center gap-2 text-sm">
            <Users className="size-4 text-muted-foreground" />
            {lead.adultCount} adult{lead.adultCount === 1 ? "" : "s"}
            {lead.childCount ? `, ${lead.childCount} child${lead.childCount === 1 ? "" : "ren"}` : ""}
            {lead.infantCount ? `, ${lead.infantCount} infant${lead.infantCount === 1 ? "" : "s"}` : ""}
            <span className="text-muted-foreground">({lead.travelerCount} billable)</span>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="font-heading text-base">Ledger</CardTitle>
          </CardHeader>
          <CardContent className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Commission per traveler</span>
              <span>{formatMoney(lead.commissionPerTraveler, { code: "EGP" })}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Commission owed</span>
              <span>{formatMoney(lead.commissionAmount, { code: "EGP" })}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Cashback owed</span>
              <span>{formatMoney(lead.cashbackAmount, { code: "EGP" })}</span>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-base">Company</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          <Link href={`/admin/companies/${lead.companyId}`} className="text-foreground hover:text-primary">
            {lead.companyName}
          </Link>
        </CardContent>
      </Card>

      <div>
        <h2 className="mb-3 font-heading text-base font-semibold">Actions</h2>
        <AdminLeadActions leadId={lead.id} availableActions={lead.availableActions ?? []} />
      </div>

      <div>
        <h2 className="mb-3 font-heading text-base font-semibold">WhatsApp</h2>
        <WhatsAppButtons leadId={lead.id} />
      </div>

      {history.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="font-heading text-base">History</CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="space-y-3">
              {history.map((h, i) => (
                <li key={i} className="flex flex-col gap-0.5 border-b border-border pb-3 text-sm last:border-0 last:pb-0">
                  <div className="flex items-center justify-between">
                    <span>
                      {h.fromStatus ?? "—"} → {h.toStatus}
                    </span>
                    <span className="text-xs text-muted-foreground">{formatDate(h.changedAt, "d MMM yyyy, HH:mm")}</span>
                  </div>
                  {h.note && <p className="text-xs text-muted-foreground">{h.note}</p>}
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
