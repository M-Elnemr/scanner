import { format, differenceInCalendarDays, parseISO } from "date-fns";

export function formatDate(iso: string | undefined | null, pattern = "d MMM yyyy"): string {
  if (!iso) return "—";
  return format(parseISO(iso), pattern);
}

export function tripDurationNights(departureDate?: string, returnDate?: string): number | null {
  if (!departureDate || !returnDate) return null;
  return differenceInCalendarDays(parseISO(returnDate), parseISO(departureDate));
}
