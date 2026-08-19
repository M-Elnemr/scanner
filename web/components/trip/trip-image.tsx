import { Landmark } from "lucide-react";
import { cn } from "@/lib/utils";

/** Falls back to a tier-keyed gradient + landmark motif when a trip's hotels have no photo yet. */
const TIER_GRADIENTS: Record<string, string> = {
  VIP: "from-amber-500 via-amber-600 to-primary",
  PREMIUM: "from-teal-500 via-primary to-teal-800",
  ECONOMIC: "from-slate-500 via-slate-600 to-slate-800",
};

export function TripImage({
  tier,
  makkahPhotoUrl,
  madinahPhotoUrl,
  className,
}: {
  tier?: string;
  makkahPhotoUrl?: string | null;
  madinahPhotoUrl?: string | null;
  className?: string;
}) {
  const photos = [makkahPhotoUrl, madinahPhotoUrl].filter((url): url is string => Boolean(url));
  if (photos.length > 0) {
    return (
      <div className={cn("relative flex overflow-hidden", className)}>
        {photos.map((url, i) => (
          // eslint-disable-next-line @next/next/no-img-element
          <img key={i} src={url} alt="" className={cn("h-full object-cover", photos.length === 2 ? "w-1/2" : "w-full")} />
        ))}
      </div>
    );
  }

  const gradient = TIER_GRADIENTS[tier ?? "ECONOMIC"] ?? TIER_GRADIENTS.ECONOMIC;
  return (
    <div
      className={cn(
        "relative flex items-center justify-center overflow-hidden bg-gradient-to-br",
        gradient,
        className,
      )}
    >
      <div className="absolute inset-0 opacity-15 [background-image:radial-gradient(circle_at_15%_15%,white,transparent_30%),radial-gradient(circle_at_85%_75%,white,transparent_25%)]" />
      <Landmark className="size-10 text-white/70" strokeWidth={1.5} />
    </div>
  );
}
