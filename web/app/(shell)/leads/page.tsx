import Link from "next/link";
import type { Metadata } from "next";
import { ArrowRight, Ticket } from "lucide-react";
import { getLocale, getTranslations } from "next-intl/server";
import { apiClient, requireUser } from "@/lib/auth/server";
import { pageableQuery } from "@/lib/api/pageable";
import { LeadStatusBadge } from "@/components/lead/lead-status-badge";
import { simplifyLeadStatus } from "@/lib/leads/status";
import { formatDate } from "@/lib/format/date";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Accordion, AccordionItem, AccordionTrigger, AccordionContent } from "@/components/ui/accordion";
import type { components } from "@/lib/api/schema";

type Lead = components["schemas"]["Lead"];

function LeadCard({ lead, locale, t }: { lead: Lead; locale: "ar" | "en"; t: Awaited<ReturnType<typeof getTranslations>> }) {
  return (
    <Link href={`/leads/${lead.id}`}>
      <Card className="transition-shadow hover:shadow-md">
        <CardContent className="flex items-center justify-between gap-4 py-4">
          <div>
            <p className="font-heading font-semibold">{lead.tripTitle}</p>
            <p className="text-xs text-muted-foreground">
              {t("preservedOn", { date: formatDate(lead.createdAt, "d MMM yyyy", locale) })}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <LeadStatusBadge status={lead.status} />
            <ArrowRight className="size-4 text-muted-foreground" />
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("leads");
  return { title: t("title") };
}

export default async function LeadsPage() {
  const user = await requireUser();
  const t = await getTranslations("leads");
  const tNav = await getTranslations("nav");
  const locale = (await getLocale()) as "ar" | "en";

  if (user.role !== "CUSTOMER") {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center text-muted-foreground">
        {t("customersOnly")}
      </div>
    );
  }

  const api = await apiClient();
  const result = await api.GET("/api/v1/customers/me/leads", {
    params: { query: pageableQuery(0, 50) },
    cache: "no-store",
  });
  const leads = result.data?.data?.content ?? [];
  const openLeads = leads.filter((lead) => simplifyLeadStatus(lead.status) === "preserved");
  const closedLeads = leads.filter((lead) => simplifyLeadStatus(lead.status) !== "preserved");

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <h1 className="mb-8 font-heading text-3xl font-bold tracking-tight">{t("title")}</h1>

      {leads.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-20 text-center">
          <Ticket className="mx-auto mb-3 size-8 text-muted-foreground" />
          <p className="mb-4 text-muted-foreground">{t("emptyText")}</p>
          <Button render={<Link href="/trips" />}>{tNav("browseTrips")}</Button>
        </div>
      ) : (
        <div className="space-y-6">
          {openLeads.length > 0 ? (
            <div className="space-y-3">
              {openLeads.map((lead) => (
                <LeadCard key={lead.id} lead={lead} locale={locale} t={t} />
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">{t("noActiveBooking")}</p>
          )}

          {closedLeads.length > 0 && (
            <Accordion>
              <AccordionItem value="closed">
                <AccordionTrigger>{t("pastBookings", { count: closedLeads.length })}</AccordionTrigger>
                <AccordionContent>
                  <div className="space-y-3">
                    {closedLeads.map((lead) => (
                      <LeadCard key={lead.id} lead={lead} locale={locale} t={t} />
                    ))}
                  </div>
                </AccordionContent>
              </AccordionItem>
            </Accordion>
          )}
        </div>
      )}
    </div>
  );
}
