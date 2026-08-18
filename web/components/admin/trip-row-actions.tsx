"use client";

import { useState, useTransition } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { ArrowRightLeft, Eye, Loader2, MoreHorizontal, Pencil, Send, Trash2, XCircle } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { changeTripStatusAction, deleteTripAction, reassignTripCompanyAction } from "@/lib/admin/trips-actions";
import type { CompanyOption } from "@/lib/admin/reference-data";

type Status = "DRAFT" | "PUBLISHED" | "CLOSED" | "EXPIRED";

export function TripRowActions({
  id,
  status,
  companies,
}: {
  id: string;
  status: string | undefined;
  companies: CompanyOption[];
}) {
  const router = useRouter();
  const t = useTranslations("admin.trips.actions");
  const tCommon = useTranslations("admin.common");
  const [pending, startTransition] = useTransition();
  const [reassignOpen, setReassignOpen] = useState(false);
  const [targetCompany, setTargetCompany] = useState("");
  const [deleteOpen, setDeleteOpen] = useState(false);

  const s = (status ?? "DRAFT") as Status;

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
          <DropdownMenuItem render={<Link href={`/trips/${id}`} target="_blank" />}>
            <Eye /> {t("viewPublic")}
          </DropdownMenuItem>
          <DropdownMenuItem render={<Link href={`/admin/trips/${id}`} />}>
            <Pencil /> {t("edit")}
          </DropdownMenuItem>
          {s === "DRAFT" && (
            <DropdownMenuItem onClick={() => run(t("publishedToast"), () => changeTripStatusAction(id, "PUBLISHED"))}>
              <Send /> {t("publish")}
            </DropdownMenuItem>
          )}
          {s === "PUBLISHED" && (
            <DropdownMenuItem onClick={() => run(t("closedToast"), () => changeTripStatusAction(id, "CLOSED"))}>
              <XCircle /> {t("close")}
            </DropdownMenuItem>
          )}
          <DropdownMenuItem onClick={() => setReassignOpen(true)}>
            <ArrowRightLeft /> {t("reassign")}
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem variant="destructive" onClick={() => setDeleteOpen(true)}>
            <Trash2 /> {tCommon("delete")}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <Dialog open={reassignOpen} onOpenChange={setReassignOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{t("reassignDialogTitle")}</DialogTitle>
            <DialogDescription>{t("reassignDialogDesc")}</DialogDescription>
          </DialogHeader>
          <div className="space-y-1.5">
            <Label>{t("newCompanyLabel")}</Label>
            <Select value={targetCompany} onValueChange={(v) => v && setTargetCompany(v)}>
              <SelectTrigger className="w-full">
                <SelectValue placeholder={t("selectCompanyPlaceholder")} />
              </SelectTrigger>
              <SelectContent>
                {companies.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {c.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <DialogFooter>
            <Button
              className="w-full"
              disabled={pending || !targetCompany}
              onClick={() =>
                run(t("reassignedToast"), () => reassignTripCompanyAction(id, targetCompany), () => setReassignOpen(false))
              }
            >
              {pending && <Loader2 className="size-4 animate-spin" />}
              {t("reassignSubmit")}
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
              onClick={() => run(t("deletedToast"), () => deleteTripAction(id), () => setDeleteOpen(false))}
            >
              {pending && <Loader2 className="size-4 animate-spin" />}
              {tCommon("delete")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
