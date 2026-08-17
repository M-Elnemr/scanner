"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { CheckCircle2, Compass, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { TravelerPicker, useTravelerPickerState } from "@/components/trip/traveler-picker";
import { cancelLeadAction, preserveTripAction } from "@/lib/leads/actions";

export interface ActiveLeadSummary {
  leadId: string;
  tripId: string;
  tripTitle: string;
}

export function PreserveFlow({
  tripId,
  tripTitle,
  isLoggedIn,
  canPreserve,
  activeLead,
}: {
  tripId: string;
  tripTitle: string;
  isLoggedIn: boolean;
  /** false for admins browsing — preserving a journey is a customer action. */
  canPreserve: boolean;
  activeLead: ActiveLeadSummary | null;
}) {
  const router = useRouter();
  const [pickerOpen, setPickerOpen] = useState(false);
  const [blockedBy, setBlockedBy] = useState<ActiveLeadSummary | null>(null);
  const [cancelReason, setCancelReason] = useState("");
  const [counts, setCounts] = useTravelerPickerState();
  const [pending, startTransition] = useTransition();

  if (!isLoggedIn) {
    return (
      <Button
        size="lg"
        className="w-full rounded-full sm:w-auto"
        render={<Link href={`/login?next=/trips/${tripId}`} />}
      >
        <Compass className="size-4" /> Sign in to preserve this journey
      </Button>
    );
  }

  if (!canPreserve) {
    return null;
  }

  if (activeLead?.tripId === tripId) {
    return (
      <Button
        size="lg"
        className="w-full rounded-full border-2 border-emerald-600 bg-emerald-600 text-white hover:bg-emerald-600/90 sm:w-auto"
        render={<Link href={`/leads/${activeLead.leadId}`} />}
      >
        <CheckCircle2 className="size-4" /> Preserved — view your journey
      </Button>
    );
  }

  function openPicker() {
    if (activeLead) {
      setBlockedBy(activeLead);
    } else {
      setPickerOpen(true);
    }
  }

  function submitPreserve() {
    startTransition(async () => {
      const result = await preserveTripAction(tripId, counts.adultCount, counts.childCount, counts.infantCount);
      if (result.ok && result.data) {
        toast.success("Journey preserved. Client service will reach you soon.");
        setPickerOpen(false);
        router.push(`/leads/${result.data.leadId}`);
        return;
      }
      if (result.code === "ACTIVE_LEAD_EXISTS" && result.activeLead) {
        setPickerOpen(false);
        setBlockedBy({
          leadId: result.activeLead.leadId,
          tripId: result.activeLead.tripId,
          tripTitle: result.activeLead.tripTitle,
        });
        return;
      }
      toast.error(result.error ?? "Could not preserve this journey.");
    });
  }

  function cancelAndContinue() {
    if (!blockedBy || !cancelReason.trim()) {
      toast.error("Tell us why you're cancelling your current journey.");
      return;
    }
    startTransition(async () => {
      const result = await cancelLeadAction(blockedBy.leadId, cancelReason.trim(), blockedBy.tripId);
      if (!result.ok) {
        toast.error(result.error ?? "Could not cancel your current journey.");
        return;
      }
      toast.success(`${blockedBy.tripTitle} cancelled.`);
      setBlockedBy(null);
      setCancelReason("");
      setPickerOpen(true);
    });
  }

  return (
    <>
      <Button size="lg" className="w-full rounded-full sm:w-auto" onClick={openPicker}>
        <Compass className="size-4" /> Preserve the journey
      </Button>

      <Dialog open={pickerOpen} onOpenChange={setPickerOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Who&apos;s travelling?</DialogTitle>
            <DialogDescription>
              We&apos;ll preserve this journey and our client service team will reach out to you.
            </DialogDescription>
          </DialogHeader>
          <TravelerPicker value={counts} onChange={setCounts} />
          <DialogFooter>
            <Button className="w-full" onClick={submitPreserve} disabled={pending}>
              {pending && <Loader2 className="size-4 animate-spin" />}
              Preserve the journey
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={Boolean(blockedBy)} onOpenChange={(open) => !open && setBlockedBy(null)}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>You already have a preserved journey</DialogTitle>
            <DialogDescription>
              You&apos;re currently preserving <strong>{blockedBy?.tripTitle}</strong>. Cancel it to preserve{" "}
              <strong>{tripTitle}</strong> instead.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-1.5">
            <Label htmlFor="cancel-reason">Reason for cancelling</Label>
            <Textarea
              id="cancel-reason"
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              placeholder="e.g. Changed my travel dates"
              rows={3}
            />
          </div>
          <DialogFooter className="gap-2 sm:gap-2">
            <Button variant="outline" className="flex-1" onClick={() => setBlockedBy(null)}>
              Keep the current one
            </Button>
            <Button variant="destructive" className="flex-1" onClick={cancelAndContinue} disabled={pending}>
              {pending && <Loader2 className="size-4 animate-spin" />}
              Cancel it and continue
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
