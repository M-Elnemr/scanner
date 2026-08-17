"use client";

import { useState } from "react";
import { Minus, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";

export interface TravelerCounts {
  adultCount: number;
  childCount: number;
  infantCount: number;
}

const ROWS: { key: keyof TravelerCounts; label: string; hint: string; min: number }[] = [
  { key: "adultCount", label: "Adults", hint: "12 years and older", min: 1 },
  { key: "childCount", label: "Children", hint: "2–11 years", min: 0 },
  { key: "infantCount", label: "Infants", hint: "Under 2 years", min: 0 },
];

export function TravelerPicker({
  value,
  onChange,
}: {
  value: TravelerCounts;
  onChange: (next: TravelerCounts) => void;
}) {
  return (
    <div className="space-y-4">
      {ROWS.map((row) => (
        <div key={row.key} className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium">{row.label}</p>
            <p className="text-xs text-muted-foreground">{row.hint}</p>
          </div>
          <div className="flex items-center gap-3">
            <Button
              type="button"
              variant="outline"
              size="icon-sm"
              className="rounded-full"
              disabled={value[row.key] <= row.min}
              onClick={() => onChange({ ...value, [row.key]: Math.max(row.min, value[row.key] - 1) })}
            >
              <Minus className="size-3.5" />
            </Button>
            <span className="w-5 text-center text-sm font-semibold tabular-nums">{value[row.key]}</span>
            <Button
              type="button"
              variant="outline"
              size="icon-sm"
              className="rounded-full"
              onClick={() => onChange({ ...value, [row.key]: value[row.key] + 1 })}
            >
              <Plus className="size-3.5" />
            </Button>
          </div>
        </div>
      ))}
    </div>
  );
}

export function useTravelerPickerState(initial?: Partial<TravelerCounts>) {
  return useState<TravelerCounts>({
    adultCount: initial?.adultCount ?? 1,
    childCount: initial?.childCount ?? 0,
    infantCount: initial?.infantCount ?? 0,
  });
}
