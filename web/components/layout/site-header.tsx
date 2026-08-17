import Link from "next/link";
import { Compass, Heart, Ticket } from "lucide-react";
import { getTranslations } from "next-intl/server";
import { getCurrentUser } from "@/lib/auth/server";
import { Button } from "@/components/ui/button";
import { UserMenu } from "@/components/layout/user-menu";
import { NotificationBell } from "@/components/layout/notification-bell";
import { MobileNav } from "@/components/layout/mobile-nav";
import { LanguageSwitcher } from "@/components/layout/language-switcher";

export async function SiteHeader() {
  const user = await getCurrentUser();
  const t = await getTranslations("nav");
  const tCommon = await getTranslations("common");

  return (
    <header className="sticky top-0 z-40 border-b border-border/60 bg-background/85 backdrop-blur supports-backdrop-blur:bg-background/60">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
        <Link href="/" className="flex items-center gap-2 font-heading text-lg font-bold tracking-tight">
          <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Compass className="size-4.5" />
          </span>
          {tCommon("appName")}
        </Link>

        <nav className="hidden items-center gap-1 md:flex">
          <Button variant="ghost" render={<Link href="/trips" />}>
            {t("browseTrips")}
          </Button>
          {user && user.role !== "ADMIN" && (
            <>
              <Button variant="ghost" render={<Link href="/leads" />}>
                <Ticket /> {t("myBookings")}
              </Button>
              <Button variant="ghost" render={<Link href="/favourites" />}>
                <Heart /> {t("favourites")}
              </Button>
            </>
          )}
        </nav>

        <div className="flex items-center gap-2">
          <LanguageSwitcher />
          {user ? (
            <>
              {user.role === "CUSTOMER" && <NotificationBell />}
              <UserMenu role={user.role} />
            </>
          ) : (
            <Button render={<Link href="/login" />}>{t("signIn")}</Button>
          )}
          <MobileNav user={user} />
        </div>
      </div>
    </header>
  );
}
