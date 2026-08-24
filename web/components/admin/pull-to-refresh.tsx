"use client";

import { useRef, useState, useTransition, type TouchEvent } from "react";
import { Loader2, ArrowDown } from "lucide-react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";

const PULL_THRESHOLD = 64;
const MAX_PULL = 96;

/**
 * A touch-only pull-to-refresh gesture for admin list screens. Only arms once the touch starts at
 * the very top of the page's scroll position — otherwise a downward swipe while scrolled into the
 * list would fight the browser's own scrolling.
 */
export function PullToRefresh({ children }: { children: React.ReactNode }) {
  const t = useTranslations("admin.common");
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [pull, setPull] = useState(0);
  const startY = useRef<number | null>(null);
  const armed = useRef(false);

  function handleTouchStart(e: TouchEvent) {
    if (pending) return;
    armed.current = window.scrollY <= 0;
    startY.current = armed.current ? e.touches[0].clientY : null;
  }

  function handleTouchMove(e: TouchEvent) {
    if (!armed.current || startY.current === null) return;
    const delta = e.touches[0].clientY - startY.current;
    if (delta <= 0) {
      setPull(0);
      return;
    }
    // Diminishing resistance the further it's pulled, capped so it can never run away.
    setPull(Math.min(MAX_PULL, delta * 0.5));
  }

  function handleTouchEnd() {
    if (armed.current && pull >= PULL_THRESHOLD) {
      startTransition(() => {
        router.refresh();
      });
    }
    armed.current = false;
    startY.current = null;
    setPull(0);
  }

  const showIndicator = pull > 0 || pending;
  const indicatorHeight = pending ? 48 : pull;
  const ready = pull >= PULL_THRESHOLD;

  return (
    <div onTouchStart={handleTouchStart} onTouchMove={handleTouchMove} onTouchEnd={handleTouchEnd}>
      <div
        className="flex items-center justify-center overflow-hidden text-muted-foreground transition-[height] duration-200 md:hidden"
        style={{ height: showIndicator ? indicatorHeight : 0 }}
        aria-live="polite"
        aria-hidden={!showIndicator}
      >
        {pending ? (
          <span className="flex items-center gap-2 text-xs font-medium">
            <Loader2 className="size-4 animate-spin" /> {t("refreshing")}
          </span>
        ) : (
          <span className="flex items-center gap-2 text-xs font-medium">
            <ArrowDown className={cn("size-4 transition-transform", ready && "rotate-180")} />
            {ready ? t("releaseToRefresh") : t("pullToRefresh")}
          </span>
        )}
      </div>
      {children}
    </div>
  );
}
