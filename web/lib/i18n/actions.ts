"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { isLocale, localeCookieName, type Locale } from "@/lib/i18n/config";

const ONE_YEAR_SECONDS = 60 * 60 * 24 * 365;

export async function setLocale(locale: Locale, pathname: string) {
  if (!isLocale(locale)) return;

  (await cookies()).set(localeCookieName, locale, {
    maxAge: ONE_YEAR_SECONDS,
    path: "/",
    sameSite: "lax",
  });

  redirect(pathname);
}
