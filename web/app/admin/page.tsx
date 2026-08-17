import Link from "next/link";
import type { Metadata } from "next";
import { Building2, Compass, Hotel, Ticket, ArrowRight } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export const metadata: Metadata = { title: "Dashboard" };

const SECTIONS = [
  {
    href: "/admin/companies",
    icon: Building2,
    title: "Companies",
    description: "Approve, edit, suspend, reinstate or delete operators.",
  },
  {
    href: "/admin/trips",
    icon: Compass,
    title: "Trips",
    description: "Create, edit and reassign trips across every company.",
  },
  {
    href: "/admin/hotels",
    icon: Hotel,
    title: "Hotels",
    description: "Manage the Makkah/Madinah catalogue trips pick from.",
  },
  {
    href: "/admin/leads",
    icon: Ticket,
    title: "Leads",
    description: "Filter bookings, override status, message customers.",
  },
];

export default function AdminDashboardPage() {
  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-heading text-2xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">Full control over companies, trips, hotels and leads.</p>
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
