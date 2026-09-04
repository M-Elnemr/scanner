"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  BarChart3,
  Building2,
  Compass,
  Hotel,
  LayoutDashboard,
  LogOut,
  Menu,
  Ticket,
} from "lucide-react";
import { useTranslations } from "next-intl";
import { logoutAction } from "@/lib/auth/actions";
import type { CurrentUser } from "@/lib/auth/server";
import { Button } from "@/components/ui/button";
import { Sheet, SheetTrigger, SheetContent, SheetHeader, SheetTitle, SheetClose } from "@/components/ui/sheet";
import { cn } from "@/lib/utils";

export function AdminShell({ user, children }: { user: CurrentUser; children: React.ReactNode }) {
  const pathname = usePathname();
  const t = useTranslations("admin");
  const tNav = useTranslations("nav");

  const NAV = [
    { href: "/admin", label: t("nav.dashboard"), icon: LayoutDashboard, exact: true },
    { href: "/admin/companies", label: t("nav.companies"), icon: Building2 },
    { href: "/admin/trips", label: t("nav.trips"), icon: Compass },
    { href: "/admin/hotels", label: t("nav.hotels"), icon: Hotel },
    { href: "/admin/leads", label: t("nav.leads"), icon: Ticket },
    { href: "/admin/analytics", label: t("nav.analytics"), icon: BarChart3 },
  ];

  return (
    <div className="flex min-h-screen bg-secondary/30">
      <aside className="hidden w-64 shrink-0 flex-col border-r border-border bg-sidebar text-sidebar-foreground md:flex">
        <div className="flex h-16 items-center gap-2 border-b border-sidebar-border px-5 font-heading text-lg font-bold">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src="/logo.png" alt="" className="h-8 w-8 object-contain" />
          {t("sidebarTitle")}
        </div>
        <nav className="flex-1 space-y-1 p-3">
          {NAV.map((item) => {
            const active = item.exact ? pathname === item.href : pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  active
                    ? "bg-sidebar-primary text-sidebar-primary-foreground"
                    : "text-sidebar-foreground/80 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                )}
              >
                <item.icon className="size-4" />
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div className="border-t border-sidebar-border p-3">
          <form action={logoutAction}>
            <Button type="submit" variant="ghost" className="w-full justify-start gap-3 text-sidebar-foreground/80">
              <LogOut className="size-4" /> {tNav("signOut")}
            </Button>
          </form>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-16 items-center justify-between border-b border-border bg-background px-4 md:px-8">
          <Sheet>
            <SheetTrigger render={<Button variant="ghost" size="icon" className="md:hidden" />}>
              <Menu className="size-5" />
            </SheetTrigger>
            <SheetContent side="left" className="w-full max-w-xs">
              <SheetHeader>
                <SheetTitle>{t("sidebarTitle")}</SheetTitle>
              </SheetHeader>
              <nav className="flex flex-col gap-1 px-4">
                {NAV.map((item) => (
                  <SheetClose
                    key={item.href}
                    render={<Link href={item.href} />}
                    className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium hover:bg-muted"
                  >
                    <item.icon className="size-4" />
                    {item.label}
                  </SheetClose>
                ))}
              </nav>
            </SheetContent>
          </Sheet>
          <p className="hidden text-sm text-muted-foreground md:block">{t("signedInAs", { id: user.userId.slice(0, 8) })}</p>
          <form action={logoutAction} className="md:hidden">
            <Button type="submit" size="sm" variant="ghost">
              <LogOut className="size-4" />
            </Button>
          </form>
        </header>
        <main className="min-w-0 flex-1 p-4 md:p-8">{children}</main>
      </div>
    </div>
  );
}
