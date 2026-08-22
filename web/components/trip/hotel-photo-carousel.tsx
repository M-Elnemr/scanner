"use client";

import { useState } from "react";
import { ChevronLeft, ChevronRight, Expand, Landmark } from "lucide-react";
import { cn } from "@/lib/utils";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";

/** Falls back to a tier-keyed gradient + landmark motif when a trip's hotels have no photo yet. */
const TIER_GRADIENTS: Record<string, string> = {
  VIP: "from-amber-500 via-amber-600 to-primary",
  PREMIUM: "from-teal-500 via-primary to-teal-800",
  ECONOMIC: "from-slate-500 via-slate-600 to-slate-800",
};

export interface HotelPhoto {
  city?: string;
  hotelName?: string;
  hotelNameAr?: string;
  photoUrl?: string;
}

interface HotelPhotoCarouselProps {
  photos: HotelPhoto[];
  tier?: string;
  locale: "ar" | "en";
  /** "card" is compact (list/grid) — dots only, no tap-to-fullscreen. "detail" adds the name overlay and lightbox. */
  variant?: "card" | "detail";
  className?: string;
}

export function HotelPhotoCarousel({ photos, tier, locale, variant = "card", className }: HotelPhotoCarouselProps) {
  const usable = photos.filter((p): p is HotelPhoto & { photoUrl: string } => Boolean(p.photoUrl));
  const [index, setIndex] = useState(0);
  const [lightboxOpen, setLightboxOpen] = useState(false);

  if (usable.length === 0) {
    const gradient = TIER_GRADIENTS[tier ?? "ECONOMIC"] ?? TIER_GRADIENTS.ECONOMIC;
    return (
      <div className={cn("relative flex items-center justify-center overflow-hidden bg-gradient-to-br", gradient, className)}>
        <div className="absolute inset-0 opacity-15 [background-image:radial-gradient(circle_at_15%_15%,white,transparent_30%),radial-gradient(circle_at_85%_75%,white,transparent_25%)]" />
        <Landmark className="size-10 text-white/70" strokeWidth={1.5} />
      </div>
    );
  }

  const name = (p: HotelPhoto) => (locale === "ar" && p.hotelNameAr ? p.hotelNameAr : p.hotelName);

  function goTo(i: number) {
    setIndex(Math.min(usable.length - 1, Math.max(0, i)));
  }

  return (
    <>
      <div className={cn("relative", className)}>
        <Track
          photos={usable}
          locale={locale}
          index={index}
          onIndexChange={goTo}
          showNameOverlay={variant === "detail"}
          onTap={variant === "detail" ? () => setLightboxOpen(true) : undefined}
        />
        {usable.length > 1 && <Dots count={usable.length} active={index} />}
        {variant === "detail" && (
          <div className="pointer-events-none absolute end-3 top-3 flex size-8 items-center justify-center rounded-full bg-black/40 text-white">
            <Expand className="size-4" />
          </div>
        )}
      </div>

      {variant === "detail" && (
        <Dialog open={lightboxOpen} onOpenChange={setLightboxOpen}>
          <DialogContent className="max-w-none w-screen h-screen max-h-screen rounded-none border-0 bg-black p-0 sm:max-w-none">
            <DialogTitle className="sr-only">{name(usable[index]) ?? ""}</DialogTitle>
            <div className="relative h-full w-full">
              <Track photos={usable} locale={locale} index={index} onIndexChange={goTo} showNameOverlay fullscreen />
              {usable.length > 1 && <Dots count={usable.length} active={index} />}
            </div>
          </DialogContent>
        </Dialog>
      )}
    </>
  );
}

function Track({
  photos,
  locale,
  index,
  onIndexChange,
  showNameOverlay,
  onTap,
  fullscreen,
}: {
  photos: (HotelPhoto & { photoUrl: string })[];
  locale: "ar" | "en";
  index: number;
  onIndexChange: (i: number) => void;
  showNameOverlay?: boolean;
  onTap?: () => void;
  fullscreen?: boolean;
}) {
  const name = (p: HotelPhoto) => (locale === "ar" && p.hotelNameAr ? p.hotelNameAr : p.hotelName);
  const canPrev = index > 0;
  const canNext = index < photos.length - 1;

  return (
    <div className={cn("relative h-full w-full overflow-hidden", fullscreen ? "flex items-center" : "")}>
      {/* Forced LTR here so arrow navigation stays spatially predictable (left = previous, right = next)
          regardless of the surrounding page's RTL direction — this strip is purely visual, not text. */}
      <div
        dir="ltr"
        className="flex h-full w-full transition-transform duration-300 ease-out"
        style={{ transform: `translateX(${index * -100}%)` }}
      >
        {photos.map((p, i) => (
          <div key={i} className="relative h-full w-full shrink-0">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={p.photoUrl}
              alt={name(p) ?? ""}
              onClick={onTap}
              className={cn("h-full w-full", fullscreen ? "object-contain" : "object-cover", onTap && "cursor-zoom-in")}
            />
            {showNameOverlay && name(p) && (
              <div className="pointer-events-none absolute inset-x-0 top-0 bg-gradient-to-b from-black/60 to-transparent p-3">
                <p className="text-sm font-semibold text-white drop-shadow-sm">{name(p)}</p>
              </div>
            )}
          </div>
        ))}
      </div>

      {photos.length > 1 && (
        <>
          <button
            type="button"
            aria-label="Previous image"
            disabled={!canPrev}
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              onIndexChange(index - 1);
            }}
            className="absolute left-2 top-1/2 flex size-8 -translate-y-1/2 items-center justify-center rounded-full bg-black/40 text-white transition-opacity hover:bg-black/60 disabled:pointer-events-none disabled:opacity-30"
          >
            <ChevronLeft className="size-5" />
          </button>
          <button
            type="button"
            aria-label="Next image"
            disabled={!canNext}
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              onIndexChange(index + 1);
            }}
            className="absolute right-2 top-1/2 flex size-8 -translate-y-1/2 items-center justify-center rounded-full bg-black/40 text-white transition-opacity hover:bg-black/60 disabled:pointer-events-none disabled:opacity-30"
          >
            <ChevronRight className="size-5" />
          </button>
        </>
      )}
    </div>
  );
}

function Dots({ count, active }: { count: number; active: number }) {
  return (
    <div className="pointer-events-none absolute inset-x-0 bottom-2 flex items-center justify-center gap-1.5">
      {Array.from({ length: count }).map((_, i) => (
        <span
          key={i}
          className={cn(
            "h-1.5 rounded-full bg-white shadow-sm transition-all",
            i === active ? "w-4 opacity-100" : "w-1.5 opacity-50",
          )}
        />
      ))}
    </div>
  );
}
