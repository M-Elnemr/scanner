"use client";

import { useState, useTransition } from "react";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { updateProfileAction } from "@/lib/customer/actions";

type WalletType = "VODAFONE_CASH" | "ETISALAT_CASH" | "INSTA_PAY";

export function ProfileForm({
  initial,
}: {
  initial: { fullName: string; phone: string; cashbackWalletNumber: string; walletType: WalletType };
}) {
  const [values, setValues] = useState(initial);
  const [pending, startTransition] = useTransition();
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const t = useTranslations("profile");

  function submit(e: React.FormEvent) {
    e.preventDefault();
    startTransition(async () => {
      const result = await updateProfileAction(values);
      if (result.ok) {
        setFieldErrors({});
        toast.success(t("updated"));
      } else {
        setFieldErrors(result.fieldErrors ?? {});
        toast.error(result.error ?? t("couldNotUpdate"));
      }
    });
  }

  return (
    <form onSubmit={submit} className="space-y-5">
      <div className="space-y-1.5">
        <Label htmlFor="fullName">{t("fullName")}</Label>
        <Input
          id="fullName"
          value={values.fullName}
          onChange={(e) => setValues((v) => ({ ...v, fullName: e.target.value }))}
          required
        />
        {fieldErrors.fullName && <p className="text-xs text-destructive">{fieldErrors.fullName}</p>}
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="phone">{t("whatsappNumber")}</Label>
        <Input
          id="phone"
          value={values.phone}
          onChange={(e) => setValues((v) => ({ ...v, phone: e.target.value }))}
          placeholder={t("phonePlaceholder")}
          required
        />
        {fieldErrors.phone && <p className="text-xs text-destructive">{fieldErrors.phone}</p>}
      </div>

      <Button type="submit" disabled={pending}>
        {pending && <Loader2 className="size-4 animate-spin" />}
        {t("saveChanges")}
      </Button>
    </form>
  );
}
