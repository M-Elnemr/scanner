import { format, differenceInCalendarDays, parseISO } from "date-fns";
import { arEG, enUS } from "date-fns/locale";

const DATE_FNS_LOCALES = { ar: arEG, en: enUS } as const;

export function formatDate(
  iso: string | undefined | null,
  pattern = "d MMM yyyy",
  locale: "ar" | "en" = "en",
): string {
  if (!iso) return "—";
  return format(parseISO(iso), pattern, { locale: DATE_FNS_LOCALES[locale] });
}

/** Whole trip days (departure day through return day inclusive) — the travel-industry convention, one more than the night count. */
export function tripDurationDays(departureDate?: string, returnDate?: string): number | null {
  if (!departureDate || !returnDate) return null;
  return differenceInCalendarDays(parseISO(returnDate), parseISO(departureDate)) + 1;
}

export function today(): string {
  return new Date().toISOString().slice(0, 10);
}
