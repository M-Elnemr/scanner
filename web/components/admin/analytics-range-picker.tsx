"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { CalendarClock } from "lucide-react";
import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";

type PresetKey = "today" | "yesterday" | "last30min" | "last24h" | "last7d" | "last30d";

const PRESETS: PresetKey[] = ["today", "yesterday", "last30min", "last24h", "last7d", "last30d"];

/** Every preset resolves in the *browser's* local time — an admin picking "today" means today
 * where they are, not UTC midnight. The resulting from/to are sent to the backend as plain
 * ISO-8601 instants (see AdminAnalyticsController), so no timezone logic is needed server-side. */
function presetRange(key: PresetKey): { from: Date; to: Date } {
  const now = new Date();
  const startOfToday = new Date(now);
  startOfToday.setHours(0, 0, 0, 0);

  switch (key) {
    case "last30min":
      return { from: new Date(now.getTime() - 30 * 60 * 1000), to: now };
    case "last24h":
      return { from: new Date(now.getTime() - 24 * 60 * 60 * 1000), to: now };
    case "today":
      return { from: startOfToday, to: now };
    case "yesterday": {
      const from = new Date(startOfToday.getTime() - 24 * 60 * 60 * 1000);
      return { from, to: startOfToday };
    }
    case "last7d":
      return { from: new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000), to: now };
    case "last30d":
      return { from: new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000), to: now };
  }
}

/** For a `datetime-local` input's value — local time, no timezone suffix. */
function toLocalInputValue(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function AnalyticsRangePicker() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const t = useTranslations("admin.analytics");
  const now = new Date();
  const [customFrom, setCustomFrom] = useState(toLocalInputValue(new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)));
  const [customTo, setCustomTo] = useState(toLocalInputValue(now));
  const [open, setOpen] = useState(false);

  const activeFrom = searchParams.get("from");
  const activeTo = searchParams.get("to");

  function applyPreset(key: PresetKey) {
    const { from, to } = presetRange(key);
    router.push(`/admin/analytics?from=${from.toISOString()}&to=${to.toISOString()}`);
  }

  function applyCustom() {
    const from = new Date(customFrom);
    const to = new Date(customTo);
    if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime())) return;
    router.push(`/admin/analytics?from=${from.toISOString()}&to=${to.toISOString()}`);
    setOpen(false);
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      {PRESETS.map((key) => (
        <Badge
          key={key}
          variant="outline"
          className="cursor-pointer rounded-full px-3 py-1.5 text-sm"
          onClick={() => applyPreset(key)}
        >
          {t(`range.${key}`)}
        </Badge>
      ))}

      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger
          render={
            <Button variant="outline" size="sm" className="rounded-full">
              <CalendarClock className="size-3.5" />
              {t("range.custom")}
            </Button>
          }
        />
        <PopoverContent>
          <div className="space-y-1.5">
            <Label>{t("range.from")}</Label>
            <Input type="datetime-local" value={customFrom} onChange={(e) => setCustomFrom(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label>{t("range.to")}</Label>
            <Input type="datetime-local" value={customTo} onChange={(e) => setCustomTo(e.target.value)} />
          </div>
          <Button size="sm" className="w-full" onClick={applyCustom}>
            {t("range.apply")}
          </Button>
        </PopoverContent>
      </Popover>

      {(activeFrom || activeTo) && (
        <Button
          variant="ghost"
          size="sm"
          className="text-muted-foreground"
          onClick={() => router.push("/admin/analytics")}
        >
          {t("range.reset")}
        </Button>
      )}
    </div>
  );
}
