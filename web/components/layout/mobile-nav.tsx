"use client";

import { useState } from "react";
import Link from "next/link";
import { Compass, Heart, Menu, Ticket } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Sheet, SheetTrigger, SheetContent, SheetHeader, SheetTitle, SheetClose } from "@/components/ui/sheet";
import type { CurrentUser } from "@/lib/auth/server";

export function MobileNav({ user }: { user: CurrentUser | null }) {
  const [open, setOpen] = useState(false);
  const t = useTranslations("nav");

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger render={<Button variant="ghost" size="icon" className="md:hidden" />}>
        <Menu className="size-5" />
      </SheetTrigger>
      <SheetContent side="left" className="w-full max-w-xs">
        <SheetHeader>
          <SheetTitle>{t("menu")}</SheetTitle>
        </SheetHeader>
        <nav className="flex flex-col gap-1 px-4">
          <SheetClose
            render={<Link href="/trips" />}
            className="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm font-medium hover:bg-muted"
          >
            <Compass className="size-4" /> {t("browseTrips")}
          </SheetClose>
          {user && user.role !== "ADMIN" && (
            <>
              <SheetClose
                render={<Link href="/leads" />}
                className="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm font-medium hover:bg-muted"
              >
                <Ticket className="size-4" /> {t("myBookings")}
              </SheetClose>
              <SheetClose
                render={<Link href="/favourites" />}
                className="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm font-medium hover:bg-muted"
              >
                <Heart className="size-4" /> {t("favourites")}
              </SheetClose>
            </>
          )}
        </nav>
      </SheetContent>
    </Sheet>
  );
}
