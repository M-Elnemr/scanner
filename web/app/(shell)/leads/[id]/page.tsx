import Link from "next/link";
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ArrowLeft, Users } from "lucide-react";
import { apiClient, requireUser } from "@/lib/auth/server";
import { LeadStatusBadge } from "@/components/lead/lead-status-badge";
import { LeadDetailActions } from "@/components/lead/lead-detail-actions";
import { ReviewCard } from "@/components/lead/review-card";
import { CompanyCard } from "@/components/trip/company-card";
import { simplifyLeadStatus } from "@/lib/leads/status";
import { Card, CardContent } from "@/components/ui/card";

export const metadata: Metadata = { title: "Booking detail" };

export default async function LeadDetailPage(props: PageProps<"/leads/[id]">) {
  const user = await requireUser();
  const { id } = await props.params;

  if (user.role !== "CUSTOMER") {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center text-muted-foreground">
        Bookings are available to customer accounts.
      </div>
    );
  }

  const api = await apiClient();
  const result = await api.GET("/api/v1/leads/{id}", { params: { path: { id } }, cache: "no-store" });
  const lead = result.data?.data;
  if (!lead?.id) notFound();

  const companyResult = lead.companyId
    ? await api.GET("/api/v1/companies/{id}", { params: { path: { id: lead.companyId } }, cache: "no-store" })
    : null;
  const company = companyResult?.data?.data ?? null;

  const state = simplifyLeadStatus(lead.status);

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <Link href="/leads" className="mb-4 flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-3.5" /> My bookings
      </Link>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">
            <Link href={`/trips/${lead.tripId}`} className="hover:text-primary">
              {lead.tripTitle}
            </Link>
          </h1>
          <p className="text-sm text-muted-foreground">Preserved journey</p>
        </div>
        <LeadStatusBadge status={lead.status} />
      </div>

      <Card className="mb-6">
        <CardContent className="flex items-center gap-2 pt-6 text-sm">
          <Users className="size-4 text-muted-foreground" />
          {lead.adultCount} adult{lead.adultCount === 1 ? "" : "s"}
          {lead.childCount ? `, ${lead.childCount} child${lead.childCount === 1 ? "" : "ren"}` : ""}
          {lead.infantCount ? `, ${lead.infantCount} infant${lead.infantCount === 1 ? "" : "s"}` : ""}
        </CardContent>
      </Card>

      {state === "cancelled" && (
        <Card className="mb-8 border-dashed">
          <CardContent className="py-6 text-center text-sm text-muted-foreground">
            This journey was cancelled.{" "}
            <Link href="/trips" className="text-primary hover:underline">
              Browse trips
            </Link>{" "}
            to preserve another one.
          </CardContent>
        </Card>
      )}

      {state !== "cancelled" && lead.id && (
        <div className="mb-8">
          <LeadDetailActions
            leadId={lead.id}
            tripId={lead.tripId ?? ""}
            availableActions={lead.availableActions ?? []}
            travelersEditable={Boolean(lead.travelersEditable)}
            initialCounts={{
              adultCount: lead.adultCount ?? 1,
              childCount: lead.childCount ?? 0,
              infantCount: lead.infantCount ?? 0,
            }}
          />
        </div>
      )}

      {company && <div className="mb-8">{<CompanyCard company={company} />}</div>}

      {state !== "cancelled" && lead.id && <ReviewCard leadId={lead.id} />}
    </div>
  );
}
