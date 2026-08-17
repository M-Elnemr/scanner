"use client";

import { useState, useTransition } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Ban, CheckCircle2, Coins, Loader2, MoreHorizontal, Pencil, RotateCcw, Trash2, XCircle } from "lucide-react";
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
        toast.error(result.error ?? "Something went wrong.");
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
            <Pencil /> Edit profile
          </DropdownMenuItem>
          {s === "PENDING" && (
            <DropdownMenuItem onClick={() => run("Company approved.", () => approveCompanyAction(id))}>
              <CheckCircle2 /> Approve
            </DropdownMenuItem>
          )}
          {s === "PENDING" && (
            <DropdownMenuItem onClick={() => setReasonMode("reject")}>
              <XCircle /> Reject
            </DropdownMenuItem>
          )}
          {s === "APPROVED" && (
            <DropdownMenuItem onClick={() => setReasonMode("suspend")}>
              <Ban /> Suspend
            </DropdownMenuItem>
          )}
          {s === "SUSPENDED" && (
            <DropdownMenuItem onClick={() => run("Company reinstated.", () => reinstateCompanyAction(id))}>
              <RotateCcw /> Reinstate
            </DropdownMenuItem>
          )}
          <DropdownMenuItem onClick={() => setCommissionOpen(true)}>
            <Coins /> Set commission
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem variant="destructive" onClick={() => setDeleteOpen(true)}>
            <Trash2 /> Delete
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <Dialog open={reasonMode !== null} onOpenChange={(open) => !open && setReasonMode(null)}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{reasonMode === "reject" ? "Reject this company?" : "Suspend this company?"}</DialogTitle>
            <DialogDescription>
              {reasonMode === "suspend"
                ? "Suspending hides it from new bookings immediately; existing leads are unaffected."
                : "The company will be notified with this reason."}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-1.5">
            <Label htmlFor="reason">Reason</Label>
            <Textarea id="reason" value={reason} onChange={(e) => setReason(e.target.value)} rows={3} />
          </div>
          <DialogFooter className="gap-2 sm:gap-2">
            <Button variant="outline" className="flex-1" onClick={() => setReasonMode(null)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              className="flex-1"
              disabled={pending || !reason.trim()}
              onClick={() =>
                run(
                  reasonMode === "reject" ? "Company rejected." : "Company suspended.",
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
              Confirm
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={commissionOpen} onOpenChange={setCommissionOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Set commission per traveler</DialogTitle>
            <DialogDescription>Applies to leads created after this change — existing leads keep their price.</DialogDescription>
          </DialogHeader>
          <div className="space-y-1.5">
            <Label htmlFor="commission">EGP per traveler</Label>
            <Input id="commission" type="number" min={0} value={commission} onChange={(e) => setCommission(e.target.value)} />
          </div>
          <DialogFooter>
            <Button
              className="w-full"
              disabled={pending || !commission}
              onClick={() =>
                run("Commission updated.", () => setCommissionAction(id, Number(commission)), () => setCommissionOpen(false))
              }
            >
              {pending && <Loader2 className="size-4 animate-spin" />}
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Delete this company?</DialogTitle>
            <DialogDescription>
              Refused if it has live bookings or an unsettled commission. Otherwise this soft-deletes the
              company, its trips, and suspends the owner&apos;s account. This can&apos;t be undone from here.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="gap-2 sm:gap-2">
            <Button variant="outline" className="flex-1" onClick={() => setDeleteOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              className="flex-1"
              disabled={pending}
              onClick={() => run("Company deleted.", () => deleteCompanyAction(id), () => setDeleteOpen(false))}
            >
              {pending && <Loader2 className="size-4 animate-spin" />}
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
