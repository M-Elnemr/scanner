"use client";

import { useState, useTransition } from "react";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { updateProfileAction } from "@/lib/customer/actions";

type WalletType = "VODAFONE_CASH" | "ETISALAT_CASH" | "INSTA_PAY";

const WALLET_LABEL_KEY: Record<WalletType, "walletVodafoneCash" | "walletEtisalatCash" | "walletInstaPay"> = {
  VODAFONE_CASH: "walletVodafoneCash",
  ETISALAT_CASH: "walletEtisalatCash",
  INSTA_PAY: "walletInstaPay",
};

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
        <Label htmlFor="phone">{t("phone")}</Label>
        <Input
          id="phone"
          value={values.phone}
          onChange={(e) => setValues((v) => ({ ...v, phone: e.target.value }))}
          placeholder={t("phonePlaceholder")}
          required
        />
        {fieldErrors.phone && <p className="text-xs text-destructive">{fieldErrors.phone}</p>}
      </div>

      <div className="space-y-1.5">
        <Label>{t("payoutWallet")}</Label>
        <p className="text-xs text-muted-foreground">{t("payoutWalletHint")}</p>
        <div className="flex gap-2">
          <Select
            value={values.walletType}
            onValueChange={(v) => v && setValues((val) => ({ ...val, walletType: v as WalletType }))}
            items={(Object.keys(WALLET_LABEL_KEY) as WalletType[]).map((type) => ({
              value: type,
              label: t(WALLET_LABEL_KEY[type]),
            }))}
          >
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {(Object.keys(WALLET_LABEL_KEY) as WalletType[]).map((type) => (
                <SelectItem key={type} value={type}>
                  {t(WALLET_LABEL_KEY[type])}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Input
            value={values.cashbackWalletNumber}
            onChange={(e) => setValues((v) => ({ ...v, cashbackWalletNumber: e.target.value }))}
            placeholder={t("walletNumberPlaceholder")}
            className="flex-1"
            required
          />
        </div>
        {fieldErrors.cashbackWalletNumber && (
          <p className="text-xs text-destructive">{fieldErrors.cashbackWalletNumber}</p>
        )}
      </div>

      <Button type="submit" disabled={pending}>
        {pending && <Loader2 className="size-4 animate-spin" />}
        {t("saveChanges")}
      </Button>
    </form>
  );
}
