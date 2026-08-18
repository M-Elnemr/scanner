import Link from "next/link";
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ArrowLeft, Users } from "lucide-react";
import { getTranslations } from "next-intl/server";
import { apiClient, requireUser } from "@/lib/auth/server";
import { LeadStatusBadge } from "@/components/lead/lead-status-badge";
import { LeadDetailActions } from "@/components/lead/lead-detail-actions";
import { ReviewCard } from "@/components/lead/review-card";
import { simplifyLeadStatus } from "@/lib/leads/status";
import { Card, CardContent } from "@/components/ui/card";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("leadDetail");
  return { title: t("title") };
}

export default async function LeadDetailPage(props: PageProps<"/leads/[id]">) {
  const user = await requireUser();
  const { id } = await props.params;
  const t = await getTranslations("leadDetail");
  const tLeads = await getTranslations("leads");
  const tNav = await getTranslations("nav");

  if (user.role !== "CUSTOMER") {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center text-muted-foreground">
        {tLeads("customersOnly")}
      </div>
    );
  }

  const api = await apiClient();
  const result = await api.GET("/api/v1/leads/{id}", { params: { path: { id } }, cache: "no-store" });
  const lead = result.data?.data;
  if (!lead?.id) notFound();

  const state = simplifyLeadStatus(lead.status);

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <Link href="/leads" className="mb-4 flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-3.5" /> {tLeads("title")}
      </Link>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">
            <Link href={`/trips/${lead.tripId}`} className="hover:text-primary">
              {lead.tripTitle}
            </Link>
          </h1>
          <p className="text-sm text-muted-foreground">{t("preservedJourney")}</p>
        </div>
        <LeadStatusBadge status={lead.status} />
      </div>

      <Card className="mb-6">
        <CardContent className="flex items-center gap-2 pt-6 text-sm">
          <Users className="size-4 text-muted-foreground" />
          {t("adults", { count: lead.adultCount ?? 0 })}
          {lead.childCount ? t("childrenSuffix", { count: lead.childCount }) : ""}
          {lead.infantCount ? t("infantsSuffix", { count: lead.infantCount }) : ""}
        </CardContent>
      </Card>

      {state === "cancelled" && (
        <Card className="mb-8 border-dashed">
          <CardContent className="py-6 text-center text-sm text-muted-foreground">
            {t("cancelledNotice")}{" "}
            <Link href="/trips" className="text-primary hover:underline">
              {tNav("browseTrips")}
            </Link>{" "}
            {t("browseAnother")}
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

      {state !== "cancelled" && lead.id && <ReviewCard leadId={lead.id} />}
    </div>
  );
}
