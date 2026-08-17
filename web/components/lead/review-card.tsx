"use client";

import { useState, useTransition } from "react";
import { toast } from "sonner";
import { Star, Loader2 } from "lucide-react";
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

  if (submitted) {
    return (
      <Card>
        <CardContent className="py-6 text-center text-sm text-muted-foreground">
          Thanks for your review!
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-heading text-lg">Rate your experience</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex gap-1">
          {[1, 2, 3, 4, 5].map((n) => (
            <button key={n} type="button" onClick={() => setStars(n)} aria-label={`${n} stars`}>
              <Star className={cn("size-6", n <= stars ? "fill-amber-400 text-amber-400" : "text-muted-foreground")} />
            </button>
          ))}
        </div>
        <Textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder="Share how it went (optional)"
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
                toast.success("Review submitted.");
              } else if (result.code) {
                setNotYet(result.error ?? null);
              } else {
                toast.error(result.error ?? "Could not submit your review.");
              }
            })
          }
        >
          {pending && <Loader2 className="size-4 animate-spin" />}
          Submit review
        </Button>
      </CardContent>
    </Card>
  );
}
