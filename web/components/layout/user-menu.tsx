"use client";

import Link from "next/link";
import { LogOut, User as UserIcon, Heart, Ticket, LayoutDashboard, Bell } from "lucide-react";
import { useTranslations } from "next-intl";
import { logoutAction } from "@/lib/auth/actions";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export function UserMenu({ role }: { role: "CUSTOMER" | "COMPANY" | "ADMIN" }) {
  const initial = role === "ADMIN" ? "A" : "U";
  const t = useTranslations("nav");

  return (
    <DropdownMenu>
      <DropdownMenuTrigger render={<Button variant="ghost" size="icon" className="rounded-full" />}>
        <Avatar className="size-8">
          <AvatarFallback className="bg-primary text-primary-foreground text-sm">{initial}</AvatarFallback>
        </Avatar>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-52">
        {role === "ADMIN" ? (
          <DropdownMenuItem render={<Link href="/admin" />}>
            <LayoutDashboard /> {t("adminDashboard")}
          </DropdownMenuItem>
        ) : (
          <>
            <DropdownMenuItem render={<Link href="/leads" />}>
              <Ticket /> {t("myBookings")}
            </DropdownMenuItem>
            <DropdownMenuItem render={<Link href="/favourites" />}>
              <Heart /> {t("favourites")}
            </DropdownMenuItem>
            <DropdownMenuItem render={<Link href="/notifications" />}>
              <Bell /> {t("notifications")}
            </DropdownMenuItem>
            <DropdownMenuItem render={<Link href="/profile" />}>
              <UserIcon /> {t("profile")}
            </DropdownMenuItem>
          </>
        )}
        <DropdownMenuSeparator />
        <form action={logoutAction}>
          <DropdownMenuItem variant="destructive" render={<button type="submit" className="w-full" />}>
            <LogOut /> {t("signOut")}
          </DropdownMenuItem>
        </form>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
