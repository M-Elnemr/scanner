import { Landmark } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Neither Trip nor Hotel has a photo field on the backend yet (only a company logo and a hotel's
 * Maps link — see docs/admin-dashboard-brief.md). Rather than hotlink stock photography (a fragile
 * external dependency for a production app), every trip card gets a tier-keyed gradient + a simple
 * landmark motif. Swap this for real photos later without touching any caller.
 */
const TIER_GRADIENTS: Record<string, string> = {
  VIP: "from-amber-500 via-amber-600 to-primary",
  PREMIUM: "from-teal-500 via-primary to-teal-800",
  ECONOMIC: "from-slate-500 via-slate-600 to-slate-800",
};

export function TripImage({ tier, className }: { tier?: string; className?: string }) {
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
