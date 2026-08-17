import Link from "next/link";
import type { Metadata } from "next";
import { ArrowRight, Ticket } from "lucide-react";
import { apiClient, requireUser } from "@/lib/auth/server";
import { pageableQuery } from "@/lib/api/pageable";
import { LeadStatusBadge } from "@/components/lead/lead-status-badge";
import { formatDate } from "@/lib/format/date";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export const metadata: Metadata = { title: "My bookings" };

export default async function LeadsPage() {
  const user = await requireUser();

  if (user.role !== "CUSTOMER") {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center text-muted-foreground">
        Bookings are available to customer accounts.
      </div>
    );
  }

  const api = await apiClient();
  const result = await api.GET("/api/v1/customers/me/leads", {
    params: { query: pageableQuery(0, 50) },
    cache: "no-store",
  });
  const leads = result.data?.data?.content ?? [];

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <h1 className="mb-8 font-heading text-3xl font-bold tracking-tight">My bookings</h1>

      {leads.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-20 text-center">
          <Ticket className="mx-auto mb-3 size-8 text-muted-foreground" />
          <p className="mb-4 text-muted-foreground">You haven&apos;t preserved a journey yet.</p>
          <Button render={<Link href="/trips" />}>Browse trips</Button>
        </div>
      ) : (
        <div className="space-y-3">
          {leads.map((lead) => (
            <Link key={lead.id} href={`/leads/${lead.id}`}>
              <Card className="transition-shadow hover:shadow-md">
                <CardContent className="flex items-center justify-between gap-4 py-4">
                  <div>
                    <p className="font-heading font-semibold">{lead.tripTitle}</p>
                    <p className="text-xs text-muted-foreground">Preserved {formatDate(lead.createdAt, "d MMM yyyy")}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <LeadStatusBadge status={lead.status} />
                    <ArrowRight className="size-4 text-muted-foreground" />
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
