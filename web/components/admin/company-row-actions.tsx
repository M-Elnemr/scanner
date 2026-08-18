"use client";

import { useState, useTransition } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Ban, CheckCircle2, Coins, Loader2, MoreHorizontal, Pencil, RotateCcw, Trash2, XCircle } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import {
  approveCompanyAction,
  deleteCompanyAction,
  reinstateCompanyAction,
  rejectCompanyAction,
  setCommissionAction,
  suspendCompanyAction,
} from "@/lib/admin/companies-actions";

type Status = "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED";
type ReasonMode = "reject" | "suspend" | null;

export function CompanyRowActions({
  id,
  status,
  commissionPerTraveler,
}: {
  id: string;
  status: string | undefined;
  commissionPerTraveler: number | undefined;
}) {
  const router = useRouter();
  const t = useTranslations("admin.companies.actions");
  const tCommon = useTranslations("admin.common");
  const [pending, startTransition] = useTransition();
  const [reasonMode, setReasonMode] = useState<ReasonMode>(null);
  const [reason, setReason] = useState("");
  const [commissionOpen, setCommissionOpen] = useState(false);
  const [commission, setCommission] = useState(String(commissionPerTraveler ?? ""));
  const [deleteOpen, setDeleteOpen] = useState(false);

  const s = (status ?? "PENDING") as Status;

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
    <>
      <DropdownMenu>
        <DropdownMenuTrigger render={<Button variant="ghost" size="icon" />}>
          <MoreHorizontal className="size-4" />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-56">
          <DropdownMenuItem render={<Link href={`/admin/companies/${id}`} />}>
            <Pencil /> {t("editProfile")}
          </DropdownMenuItem>
          {s === "PENDING" && (
            <DropdownMenuItem onClick={() => run(t("approvedToast"), () => approveCompanyAction(id))}>
              <CheckCircle2 /> {t("approve")}
            </DropdownMenuItem>
          )}
          {s === "PENDING" && (
            <DropdownMenuItem onClick={() => setReasonMode("reject")}>
              <XCircle /> {t("reject")}
            </DropdownMenuItem>
          )}
          {s === "APPROVED" && (
            <DropdownMenuItem onClick={() => setReasonMode("suspend")}>
              <Ban /> {t("suspend")}
            </DropdownMenuItem>
          )}
          {s === "SUSPENDED" && (
            <DropdownMenuItem onClick={() => run(t("reinstatedToast"), () => reinstateCompanyAction(id))}>
              <RotateCcw /> {t("reinstate")}
            </DropdownMenuItem>
          )}
          <DropdownMenuItem onClick={() => setCommissionOpen(true)}>
            <Coins /> {t("setCommission")}
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem variant="destructive" onClick={() => setDeleteOpen(true)}>
            <Trash2 /> {t("delete")}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <Dialog open={reasonMode !== null} onOpenChange={(open) => !open && setReasonMode(null)}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{reasonMode === "reject" ? t("rejectDialogTitle") : t("suspendDialogTitle")}</DialogTitle>
            <DialogDescription>
              {reasonMode === "suspend" ? t("suspendDialogDesc") : t("rejectDialogDesc")}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-1.5">
            <Label htmlFor="reason">{t("reasonFieldLabel")}</Label>
            <Textarea id="reason" value={reason} onChange={(e) => setReason(e.target.value)} rows={3} />
          </div>
          <DialogFooter className="gap-2 sm:gap-2">
            <Button variant="outline" className="flex-1" onClick={() => setReasonMode(null)}>
              {tCommon("cancel")}
            </Button>
            <Button
              variant="destructive"
              className="flex-1"
              disabled={pending || !reason.trim()}
              onClick={() =>
                run(
                  reasonMode === "reject" ? t("rejectedToast") : t("suspendedToast"),
                  () =>
                    reasonMode === "reject"
                      ? rejectCompanyAction(id, reason.trim())
                      : suspendCompanyAction(id, reason.trim()),
                  () => {
                    setReasonMode(null);
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

      <Dialog open={commissionOpen} onOpenChange={setCommissionOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{t("setCommissionTitle")}</DialogTitle>
            <DialogDescription>{t("setCommissionDesc")}</DialogDescription>
          </DialogHeader>
          <div className="space-y-1.5">
            <Label htmlFor="commission">{t("commissionFieldLabel")}</Label>
            <Input id="commission" type="number" min={0} value={commission} onChange={(e) => setCommission(e.target.value)} />
          </div>
          <DialogFooter>
            <Button
              className="w-full"
              disabled={pending || !commission}
              onClick={() =>
                run(t("commissionUpdatedToast"), () => setCommissionAction(id, Number(commission)), () => setCommissionOpen(false))
              }
            >
              {pending && <Loader2 className="size-4 animate-spin" />}
              {tCommon("save")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{t("deleteDialogTitle")}</DialogTitle>
            <DialogDescription>{t("deleteDialogDesc")}</DialogDescription>
          </DialogHeader>
          <DialogFooter className="gap-2 sm:gap-2">
            <Button variant="outline" className="flex-1" onClick={() => setDeleteOpen(false)}>
              {tCommon("cancel")}
            </Button>
            <Button
              variant="destructive"
              className="flex-1"
              disabled={pending}
              onClick={() => run(t("deletedToast"), () => deleteCompanyAction(id), () => setDeleteOpen(false))}
            >
              {pending && <Loader2 className="size-4 animate-spin" />}
              {t("delete")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
