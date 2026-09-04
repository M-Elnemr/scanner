"use client";

import { useMemo, useState } from "react";
import { format } from "date-fns";
import { arEG, enUS } from "date-fns/locale";
import { useLocale, useTranslations } from "next-intl";

const WIDTH = 600;
const HEIGHT = 180;
const PAD_LEFT = 36;
const PAD_RIGHT = 12;
const PAD_TOP = 16;
const PAD_BOTTOM = 24;

interface Point {
  bucket: string;
  count: number;
}

/** Rounds up to a "clean" number for the y-axis max (1,000 / 2,000 / ..., never a raw count). */
function niceMax(value: number): number {
  if (value <= 0) return 1;
  const magnitude = 10 ** Math.floor(Math.log10(value));
  const normalized = value / magnitude;
  const step = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10;
  return step * magnitude;
}

/**
 * A single-series trend line for the events-over-time aggregate — no legend (one series, the
 * panel title already names it), a 2px line with a ~10%-opacity area wash, an endpoint value
 * label, three rounded y-axis ticks, and a crosshair + tooltip on hover/focus. No charting
 * library: this is the whole chart, plain SVG, kept intentionally light like the rest of this
 * admin page's tables.
 */
export function EventsOverTimeChart({ data, emptyLabel }: { data: Point[]; emptyLabel: string }) {
  const locale = useLocale() as "ar" | "en";
  const t = useTranslations("admin.analytics");
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);

  const dateFnsLocale = locale === "ar" ? arEG : enUS;

  // Buckets can be minute/hour/day-granular depending on the selected range (see
  // AnalyticsReportingService.eventsOverTime) — inferred here from the gap between the first two
  // points so axis/tooltip labels show time only when the range is short enough for it to matter.
  const showTime = useMemo(() => {
    if (data.length < 2) return true;
    const gapMs = new Date(data[1].bucket).getTime() - new Date(data[0].bucket).getTime();
    return gapMs < 24 * 60 * 60 * 1000;
  }, [data]);

  const formatLabel = (iso: string) =>
    format(new Date(iso), showTime ? "d MMM, HH:mm" : "d MMM", { locale: dateFnsLocale });

  if (data.length === 0) {
    return <p className="px-4 py-8 text-center text-sm text-muted-foreground">{emptyLabel}</p>;
  }

  const maxCount = niceMax(Math.max(...data.map((p) => p.count)));
  const plotWidth = WIDTH - PAD_LEFT - PAD_RIGHT;
  const plotHeight = HEIGHT - PAD_TOP - PAD_BOTTOM;

  const xFor = (i: number) => PAD_LEFT + (data.length === 1 ? plotWidth / 2 : (i / (data.length - 1)) * plotWidth);
  const yFor = (count: number) => PAD_TOP + plotHeight - (count / maxCount) * plotHeight;

  const linePath = data.map((p, i) => `${i === 0 ? "M" : "L"}${xFor(i)},${yFor(p.count)}`).join(" ");
  const areaPath = `${linePath} L${xFor(data.length - 1)},${PAD_TOP + plotHeight} L${xFor(0)},${PAD_TOP + plotHeight} Z`;

  const yTicks = [0, maxCount / 2, maxCount];
  const last = data[data.length - 1];
  const hovered = hoverIndex !== null ? data[hoverIndex] : null;

  return (
    <div className="px-4 py-3">
      <svg
        viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
        className="w-full touch-none"
        role="img"
        aria-label={t("eventsOverTime")}
        onPointerMove={(e) => {
          const rect = e.currentTarget.getBoundingClientRect();
          const relativeX = ((e.clientX - rect.left) / rect.width) * WIDTH;
          const ratio = data.length === 1 ? 0 : (relativeX - PAD_LEFT) / plotWidth;
          const index = Math.round(ratio * (data.length - 1));
          setHoverIndex(Math.min(Math.max(index, 0), data.length - 1));
        }}
        onPointerLeave={() => setHoverIndex(null)}
      >
        {/* Y-axis ticks — recessive gray, hairline baseline only (no full gridlines, one series). */}
        {yTicks.map((value) => (
          <g key={value}>
            <text
              x={PAD_LEFT - 8}
              y={yFor(value)}
              textAnchor="end"
              dominantBaseline="middle"
              className="fill-muted-foreground text-[9px] tabular-nums"
            >
              {Math.round(value).toLocaleString()}
            </text>
          </g>
        ))}
        <line
          x1={PAD_LEFT}
          y1={PAD_TOP + plotHeight}
          x2={WIDTH - PAD_RIGHT}
          y2={PAD_TOP + plotHeight}
          className="stroke-border"
          strokeWidth={1}
        />

        <path d={areaPath} className="fill-chart-1" opacity={0.1} />
        <path d={linePath} fill="none" className="stroke-chart-1" strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" />

        {/* Endpoint marker + value label — the one point labeled directly, per the "label
            selectively" rule; every other value lives in the tooltip. */}
        <circle cx={xFor(data.length - 1)} cy={yFor(last.count)} r={4} className="fill-chart-1 stroke-background" strokeWidth={2} />
        <text
          x={xFor(data.length - 1)}
          y={yFor(last.count) - 8}
          textAnchor="end"
          className="fill-foreground text-[10px] font-medium tabular-nums"
        >
          {last.count.toLocaleString()}
        </text>

        {/* Crosshair + hovered point */}
        {hovered && (
          <>
            <line
              x1={xFor(hoverIndex!)}
              y1={PAD_TOP}
              x2={xFor(hoverIndex!)}
              y2={PAD_TOP + plotHeight}
              className="stroke-muted-foreground"
              strokeWidth={1}
              strokeDasharray="2,2"
            />
            <circle
              cx={xFor(hoverIndex!)}
              cy={yFor(hovered.count)}
              r={4}
              className="fill-chart-1 stroke-background"
              strokeWidth={2}
            />
          </>
        )}

        <text x={PAD_LEFT} y={HEIGHT - 4} className="fill-muted-foreground text-[9px]">
          {formatLabel(data[0].bucket)}
        </text>
        <text x={WIDTH - PAD_RIGHT} y={HEIGHT - 4} textAnchor="end" className="fill-muted-foreground text-[9px]">
          {formatLabel(data[data.length - 1].bucket)}
        </text>
      </svg>

      {hovered && (
        <div className="mt-1 flex items-center gap-2 text-xs">
          <span className="inline-block h-0.5 w-3 rounded-full bg-chart-1" />
          <span className="text-muted-foreground">{formatLabel(hovered.bucket)}</span>
          <span className="font-medium tabular-nums">{hovered.count.toLocaleString()}</span>
        </div>
      )}

      <details className="mt-2 text-xs text-muted-foreground">
        <summary className="cursor-pointer select-none">{t("range.viewAsTable")}</summary>
        <table className="mt-2 w-full">
          <tbody>
            {data.map((p) => (
              <tr key={p.bucket} className="border-t border-border/50">
                <td className="py-1">{formatLabel(p.bucket)}</td>
                <td className="py-1 text-end tabular-nums">{p.count.toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </div>
  );
}
