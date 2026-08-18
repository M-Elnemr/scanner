"use client";

import { useState, useTransition } from "react";
import { toast } from "sonner";
import { Heart } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { toggleFavouriteAction } from "@/lib/leads/actions";
import { cn } from "@/lib/utils";

export function FavouriteButton({ tripId, initialFavourited }: { tripId: string; initialFavourited: boolean }) {
  const [favourited, setFavourited] = useState(initialFavourited);
  const [pending, startTransition] = useTransition();
  const t = useTranslations("favourites");

  return (
    <Button
      variant="outline"
      size="icon"
      className="rounded-full"
      disabled={pending}
      onClick={() =>
        startTransition(async () => {
          const next = !favourited;
          setFavourited(next);
          const result = await toggleFavouriteAction(tripId, favourited);
          if (!result.ok) {
            setFavourited(!next);
            toast.error(result.error ?? t("couldNotUpdate"));
          }
        })
      }
      aria-label={favourited ? t("removeFromFavourites") : t("addToFavourites")}
    >
      <Heart className={cn("size-4", favourited && "fill-destructive text-destructive")} />
    </Button>
  );
}
