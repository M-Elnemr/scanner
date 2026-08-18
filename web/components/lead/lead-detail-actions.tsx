"use client";

import { useState, useTransition } from "react";
import { toast } from "sonner";
import { CreditCard, Loader2, Pencil, Wallet, X } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { TravelerPicker, useTravelerPickerState } from "@/components/trip/traveler-picker";
import {
  cancelLeadAction,
  reportDepositAction,
  reportFullPaymentAction,
  updateTravelersAction,
} from "@/lib/leads/actions";

type Action = "REPORT_DEPOSIT" | "REPORT_FULL_PAYMENT" | "CANCEL";

export function LeadDetailActions({
  leadId,
  tripId,
  availableActions,
  travelersEditable,
  initialCounts,
}: {
  leadId: string;
  tripId: string;
  availableActions: string[];
  travelersEditable: boolean;
  initialCounts: { adultCount: number; childCount: number; infantCount: number };
}) {
  const [pending, startTransition] = useTransition();
  const [editOpen, setEditOpen] = useState(false);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [counts, setCounts] = useTravelerPickerState(initialCounts);
  const t = useTranslations("leadActions");

  const actions = new Set(availableActions as Action[]);

  function run(label: string, fn: () => Promise<{ ok: boolean; error?: string }>) {
    startTransition(async () => {
      const result = await fn();
      if (result.ok) toast.success(label);
      else toast.error(result.error ?? t("genericError"));
    });
  }

  return (
    <div className="flex flex-wrap gap-2">
      {actions.has("REPORT_DEPOSIT") && (
        <Button
          variant="outline"
          disabled={pending}
          onClick={() => run(t("depositToastSuccess"), () => reportDepositAction(leadId))}
        >
          {pending ? <Loader2 className="size-4 animate-spin" /> : <Wallet className="size-4" />}
          {t("iPaidDeposit")}
        </Button>
      )}
      {actions.has("REPORT_FULL_PAYMENT") && (
        <Button
          variant="outline"
          disabled={pending}
          onClick={() => run(t("fullPaymentToastSuccess"), () => reportFullPaymentAction(leadId))}
        >
          {pending ? <Loader2 className="size-4 animate-spin" /> : <CreditCard className="size-4" />}
          {t("iPaidInFull")}
        </Button>
      )}
      {travelersEditable && (
        <Button variant="outline" onClick={() => setEditOpen(true)}>
          <Pencil className="size-4" /> {t("editTravellers")}
        </Button>
      )}
      {actions.has("CANCEL") && (
        <Button variant="destructive" onClick={() => setCancelOpen(true)}>
          <X className="size-4" /> {t("cancelJourney")}
        </Button>
      )}

      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{t("editTravellers")}</DialogTitle>
            <DialogDescription>{t("editTravellersDescription")}</DialogDescription>
          </DialogHeader>
          <TravelerPicker value={counts} onChange={setCounts} />
          <DialogFooter>
            <Button
              className="w-full"
              disabled={pending}
              onClick={() =>
                startTransition(async () => {
                  const result = await updateTravelersAction(leadId, counts.adultCount, counts.childCount, counts.infantCount);
                  if (result.ok) {
                    toast.success(t("travellersUpdated"));
                    setEditOpen(false);
                  } else {
                    toast.error(result.error ?? t("couldNotUpdateTravellers"));
                  }
                })
              }
            >
              {pending && <Loader2 className="size-4 animate-spin" />}
              {t("save")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={cancelOpen} onOpenChange={setCancelOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{t("cancelDialogTitle")}</DialogTitle>
            <DialogDescription>{t("cancelDialogDescription")}</DialogDescription>
          </DialogHeader>
          <div className="space-y-1.5">
            <Label htmlFor="cancel-reason-lead">{t("reasonLabel")}</Label>
            <Textarea
              id="cancel-reason-lead"
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              placeholder={t("reasonPlaceholder")}
              rows={3}
            />
          </div>
          <DialogFooter className="gap-2 sm:gap-2">
            <Button variant="outline" className="flex-1" onClick={() => setCancelOpen(false)}>
              {t("keepIt")}
            </Button>
            <Button
              variant="destructive"
              className="flex-1"
              disabled={pending || !cancelReason.trim()}
              onClick={() =>
                startTransition(async () => {
                  const result = await cancelLeadAction(leadId, cancelReason.trim(), tripId);
                  if (result.ok) {
                    toast.success(t("journeyCancelled"));
                    setCancelOpen(false);
                  } else {
                    toast.error(result.error ?? t("couldNotCancel"));
                  }
                })
              }
            >
              {pending && <Loader2 className="size-4 animate-spin" />}
              {t("cancelJourneyButton")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
