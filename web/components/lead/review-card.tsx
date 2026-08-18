"use client";

import { useState, useTransition } from "react";
import { toast } from "sonner";
import { Star, Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { submitRatingAction } from "@/lib/leads/actions";
import { cn } from "@/lib/utils";

export function ReviewCard({ leadId }: { leadId: string }) {
  const [stars, setStars] = useState(5);
  const [comment, setComment] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [notYet, setNotYet] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();
  const t = useTranslations("reviewCard");

  if (submitted) {
    return (
      <Card>
        <CardContent className="py-6 text-center text-sm text-muted-foreground">
          {t("thanks")}
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-heading text-lg">{t("title")}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex gap-1">
          {[1, 2, 3, 4, 5].map((n) => (
            <button key={n} type="button" onClick={() => setStars(n)} aria-label={t("starsAria", { count: n })}>
              <Star className={cn("size-6", n <= stars ? "fill-amber-400 text-amber-400" : "text-muted-foreground")} />
            </button>
          ))}
        </div>
        <Textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder={t("commentPlaceholder")}
          rows={3}
        />
        {notYet && <p className="text-sm text-muted-foreground">{notYet}</p>}
        <Button
          disabled={pending}
          onClick={() =>
            startTransition(async () => {
              const result = await submitRatingAction(leadId, stars, comment.trim() || undefined);
              if (result.ok) {
                setSubmitted(true);
                toast.success(t("submitted"));
              } else if (result.code) {
                setNotYet(result.error ?? null);
              } else {
                toast.error(result.error ?? t("couldNotSubmit"));
              }
            })
          }
        >
          {pending && <Loader2 className="size-4 animate-spin" />}
          {t("submit")}
        </Button>
      </CardContent>
    </Card>
  );
}
