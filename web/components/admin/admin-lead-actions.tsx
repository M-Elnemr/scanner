"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Banknote, CheckCircle2, Loader2, ShieldAlert } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { confirmCommissionAction, overrideLeadStatusAction, payCashbackAction } from "@/lib/admin/leads-actions";
import { ALL_LEAD_STATUSES, useLeadStatusLabel, type LeadStatus } from "@/components/admin/admin-lead-status-badge";

export function AdminLeadActions({ leadId, availableActions }: { leadId: string; availableActions: string[] }) {
  const router = useRouter();
  const t = useTranslations("admin.leads.actions");
  const tCommon = useTranslations("admin.common");
  const leadStatusLabel = useLeadStatusLabel();
  const [pending, startTransition] = useTransition();
  const [overrideOpen, setOverrideOpen] = useState(false);
  const [targetStatus, setTargetStatus] = useState<LeadStatus>("INTERESTED");
  const [reason, setReason] = useState("");

  const actions = new Set(availableActions);

  function run(label: string, fn: () => Promise<{ ok: boolean; error?: string }>, after?: () => void) {
    startTransition(async () => {
      const result = await fn();
      if (result.ok) {
        toast.success(label);
        after?.();
        router.refresh();
      } else {
        toast.error(result.error ?? tCommon("somethingWentWrong"));
      }
    });
  }

  return (
    <div className="flex flex-wrap gap-2">
      {actions.has("CONFIRM_COMMISSION_PAID") && (
        <Button
          variant="outline"
          disabled={pending}
          onClick={() => run(t("confirmedToast"), () => confirmCommissionAction(leadId))}
        >
          {pending ? <Loader2 className="size-4 animate-spin" /> : <CheckCircle2 className="size-4" />}
          {t("confirmCommission")}
        </Button>
      )}
      {actions.has("PAY_CASHBACK") && (
        <Button variant="outline" disabled={pending} onClick={() => run(t("paidToast"), () => payCashbackAction(leadId))}>
          {pending ? <Loader2 className="size-4 animate-spin" /> : <Banknote className="size-4" />}
          {t("payCashback")}
        </Button>
      )}
      <Button variant="outline" onClick={() => setOverrideOpen(true)}>
        <ShieldAlert className="size-4" /> {t("overrideStatus")}
      </Button>

      <Dialog open={overrideOpen} onOpenChange={setOverrideOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{t("overrideDialogTitle")}</DialogTitle>
            <DialogDescription>{t("overrideDialogDesc")}</DialogDescription>
          </DialogHeader>
          <div className="space-y-1.5">
            <Label>{t("newStatusLabel")}</Label>
            <Select value={targetStatus} onValueChange={(v) => v && setTargetStatus(v as LeadStatus)}>
              <SelectTrigger className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {ALL_LEAD_STATUSES.map((s) => (
                  <SelectItem key={s} value={s}>
                    {leadStatusLabel(s)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="override-reason">{t("reasonLabel")}</Label>
            <Textarea id="override-reason" value={reason} onChange={(e) => setReason(e.target.value)} rows={3} />
          </div>
          <DialogFooter className="gap-2 sm:gap-2">
            <Button variant="outline" className="flex-1" onClick={() => setOverrideOpen(false)}>
              {tCommon("cancel")}
            </Button>
            <Button
              variant="destructive"
              className="flex-1"
              disabled={pending || !reason.trim()}
              onClick={() =>
                run(
                  t("overriddenToast"),
                  () => overrideLeadStatusAction(leadId, targetStatus, reason.trim()),
                  () => {
                    setOverrideOpen(false);
                    setReason("");
                  },
                )
              }
            >
              {pending && <Loader2 className="size-4 animate-spin" />}
              {tCommon("confirm")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
