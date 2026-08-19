"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { CheckCircle2, Compass, Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
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
  const t = useTranslations("preserveFlow");
  const [pickerOpen, setPickerOpen] = useState(false);
  const [blockedBy, setBlockedBy] = useState<ActiveLeadSummary | null>(null);
  const [profileIncomplete, setProfileIncomplete] = useState(false);
  const [counts, setCounts] = useTravelerPickerState();
  const [pending, startTransition] = useTransition();

  if (!isLoggedIn) {
    return (
      <Button
        size="lg"
        className="w-full rounded-full sm:w-auto"
        render={<Link href={`/login?next=/trips/${tripId}`} />}
      >
        <Compass className="size-4" /> {t("signIn")}
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
        <CheckCircle2 className="size-4" /> {t("preservedView")}
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
        toast.success(t("toastPreserved"));
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
      if (result.code === "PROFILE_INCOMPLETE") {
        setPickerOpen(false);
        setProfileIncomplete(true);
        return;
      }
      toast.error(result.error ?? t("toastCouldNotPreserve"));
    });
  }

  function cancelAndContinue() {
    if (!blockedBy) return;
    startTransition(async () => {
      const result = await cancelLeadAction(blockedBy.leadId, undefined, blockedBy.tripId);
      if (!result.ok) {
        toast.error(result.error ?? t("toastCouldNotCancel"));
        return;
      }
      toast.success(t("toastCancelled", { title: blockedBy.tripTitle }));
      setBlockedBy(null);
      setPickerOpen(true);
    });
  }

  return (
    <>
      <Button size="lg" className="w-full rounded-full sm:w-auto" onClick={openPicker}>
        <Compass className="size-4" /> {t("preserve")}
      </Button>

      <Dialog open={pickerOpen} onOpenChange={setPickerOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{t("whoTravelling")}</DialogTitle>
            <DialogDescription>{t("weWillReachOut")}</DialogDescription>
          </DialogHeader>
          <TravelerPicker value={counts} onChange={setCounts} />
          <DialogFooter>
            <Button className="w-full" onClick={submitPreserve} disabled={pending}>
              {pending && <Loader2 className="size-4 animate-spin" />}
              {t("preserve")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={Boolean(blockedBy)} onOpenChange={(open) => !open && setBlockedBy(null)}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{t("alreadyHaveTitle")}</DialogTitle>
            <DialogDescription>
              {t.rich("currentlyPreserving", {
                current: blockedBy?.tripTitle ?? "",
                next: tripTitle,
                strong: (chunks) => <strong>{chunks}</strong>,
              })}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="gap-2 sm:gap-2">
            <Button variant="outline" className="flex-1" onClick={() => setBlockedBy(null)}>
              {t("keepCurrent")}
            </Button>
            <Button variant="destructive" className="flex-1" onClick={cancelAndContinue} disabled={pending}>
              {pending && <Loader2 className="size-4 animate-spin" />}
              {t("cancelAndContinue")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={profileIncomplete} onOpenChange={setProfileIncomplete}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{t("completeProfileTitle")}</DialogTitle>
            <DialogDescription>{t("completeProfileDescription")}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button className="w-full" render={<Link href="/profile" />}>
              {t("completeProfileCta")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
