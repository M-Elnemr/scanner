import Link from "next/link";
import type { Metadata } from "next";
import { Building2, Compass, Hotel, Ticket, ArrowRight } from "lucide-react";
import { getTranslations } from "next-intl/server";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("admin.pageTitle");
  return { title: t("dashboard") };
}

export default async function AdminDashboardPage() {
  const t = await getTranslations("admin.dashboard");

  const SECTIONS = [
    {
      href: "/admin/companies",
      icon: Building2,
      title: t("companiesTitle"),
      description: t("companiesDescription"),
    },
    {
      href: "/admin/trips",
      icon: Compass,
      title: t("tripsTitle"),
      description: t("tripsDescription"),
    },
    {
      href: "/admin/hotels",
      icon: Hotel,
      title: t("hotelsTitle"),
      description: t("hotelsDescription"),
    },
    {
      href: "/admin/leads",
      icon: Ticket,
      title: t("leadsTitle"),
      description: t("leadsDescription"),
    },
  ];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-heading text-2xl font-bold tracking-tight">{t("title")}</h1>
        <p className="text-muted-foreground">{t("subtitle")}</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {SECTIONS.map((section) => (
          <Link key={section.href} href={section.href}>
            <Card className="group h-full transition-shadow hover:shadow-md">
              <CardHeader>
                <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  <section.icon className="size-5" />
                </div>
                <CardTitle className="flex items-center justify-between pt-2">
                  {section.title}
                  <ArrowRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-1" />
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">{section.description}</p>
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
