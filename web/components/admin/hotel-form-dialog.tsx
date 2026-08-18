"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { createHotelAction, updateHotelAction, type HotelInput } from "@/lib/admin/hotels-actions";

interface HotelDraft {
  city: "MAKKAH" | "MADINAH";
  name: string;
  nameAr: string;
  stars: string;
  distanceToHaramM: string;
  canWalk: boolean;
  locationUrl: string;
  active: boolean;
}

const EMPTY: HotelDraft = {
  city: "MAKKAH",
  name: "",
  nameAr: "",
  stars: "",
  distanceToHaramM: "",
  canWalk: false,
  locationUrl: "",
  active: true,
};

export function HotelFormDialog({
  mode,
  hotelId,
  initial,
  trigger,
}: {
  mode: "create" | "edit";
  hotelId?: string;
  initial?: Partial<HotelDraft>;
  trigger: React.ReactNode;
}) {
  const router = useRouter();
  const t = useTranslations("admin.hotels.form");
  const tCommon = useTranslations("admin.common");
  const [open, setOpen] = useState(false);
  const [pending, startTransition] = useTransition();
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [form, setForm] = useState<HotelDraft>({ ...EMPTY, ...initial });

  function submit(e: React.FormEvent) {
    e.preventDefault();
    const input: HotelInput = {
      city: form.city,
      name: form.name,
      nameAr: form.nameAr || undefined,
      stars: form.stars ? Number(form.stars) : undefined,
      distanceToHaramM: form.distanceToHaramM ? Number(form.distanceToHaramM) : undefined,
      canWalk: form.canWalk,
      locationUrl: form.locationUrl || undefined,
      active: form.active,
    };
    startTransition(async () => {
      const result = mode === "create" ? await createHotelAction(input) : await updateHotelAction(hotelId!, input);
      if (result.ok) {
        setFieldErrors({});
        toast.success(mode === "create" ? t("addedToast") : t("updatedToast"));
        setOpen(false);
        router.refresh();
      } else {
        setFieldErrors(result.fieldErrors ?? {});
        toast.error(result.error ?? tCommon("somethingWentWrong"));
      }
    });
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) setForm({ ...EMPTY, ...initial });
      }}
    >
      <DialogTrigger render={<span />}>{trigger}</DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{mode === "create" ? t("addTitle") : t("editTitle")}</DialogTitle>
          <DialogDescription>{t("subtitle")}</DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label>{t("cityLabel")}</Label>
              <Select value={form.city} onValueChange={(v) => v && setForm((f) => ({ ...f, city: v as "MAKKAH" | "MADINAH" }))}>
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="MAKKAH">{t("cityMakkah")}</SelectItem>
                  <SelectItem value="MADINAH">{t("cityMadinah")}</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="stars">{t("starsLabel")}</Label>
              <Input
                id="stars"
                type="number"
                min={1}
                max={7}
                value={form.stars}
                onChange={(e) => setForm((f) => ({ ...f, stars: e.target.value }))}
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="hotelName">{t("nameLabel")}</Label>
            <Input id="hotelName" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} required />
            {fieldErrors.name && <p className="text-xs text-destructive">{fieldErrors.name}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="hotelNameAr">{t("nameArLabel")}</Label>
            <Input id="hotelNameAr" value={form.nameAr} onChange={(e) => setForm((f) => ({ ...f, nameAr: e.target.value }))} />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="distance">{t("distanceLabel")}</Label>
              <Input
                id="distance"
                type="number"
                min={0}
                value={form.distanceToHaramM}
                onChange={(e) => setForm((f) => ({ ...f, distanceToHaramM: e.target.value }))}
              />
            </div>
            <div className="flex items-center justify-between rounded-lg border border-border px-3">
              <Label htmlFor="canWalk" className="text-sm">
                {t("walkableLabel")}
              </Label>
              <Switch id="canWalk" checked={form.canWalk} onCheckedChange={(v) => setForm((f) => ({ ...f, canWalk: v }))} />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="locationUrl">{t("mapsLinkLabel")}</Label>
            <Input
              id="locationUrl"
              value={form.locationUrl}
              onChange={(e) => setForm((f) => ({ ...f, locationUrl: e.target.value }))}
            />
          </div>

          <div className="flex items-center justify-between rounded-lg border border-border px-3 py-2">
            <div>
              <Label htmlFor="active" className="text-sm">
                {t("activeLabel")}
              </Label>
              <p className="text-xs text-muted-foreground">{t("activeHint")}</p>
            </div>
            <Switch id="active" checked={form.active} onCheckedChange={(v) => setForm((f) => ({ ...f, active: v }))} />
          </div>

          <DialogFooter>
            <Button type="submit" className="w-full" disabled={pending}>
              {pending && <Loader2 className="size-4 animate-spin" />}
              {mode === "create" ? t("addSubmit") : t("saveSubmit")}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
