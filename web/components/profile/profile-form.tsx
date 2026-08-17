"use client";

import { useState, useTransition } from "react";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { updateProfileAction } from "@/lib/customer/actions";

type WalletType = "VODAFONE_CASH" | "ETISALAT_CASH" | "INSTA_PAY";

const WALLET_LABEL: Record<WalletType, string> = {
  VODAFONE_CASH: "Vodafone Cash",
  ETISALAT_CASH: "Etisalat Cash",
  INSTA_PAY: "InstaPay",
};

export function ProfileForm({
  initial,
}: {
  initial: { fullName: string; phone: string; cashbackWalletNumber: string; walletType: WalletType };
}) {
  const [values, setValues] = useState(initial);
  const [pending, startTransition] = useTransition();
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  function submit(e: React.FormEvent) {
    e.preventDefault();
    startTransition(async () => {
      const result = await updateProfileAction(values);
      if (result.ok) {
        setFieldErrors({});
        toast.success("Profile updated.");
      } else {
        setFieldErrors(result.fieldErrors ?? {});
        toast.error(result.error ?? "Could not update your profile.");
      }
    });
  }

  return (
    <form onSubmit={submit} className="space-y-5">
      <div className="space-y-1.5">
        <Label htmlFor="fullName">Full name</Label>
        <Input
          id="fullName"
          value={values.fullName}
          onChange={(e) => setValues((v) => ({ ...v, fullName: e.target.value }))}
          required
        />
        {fieldErrors.fullName && <p className="text-xs text-destructive">{fieldErrors.fullName}</p>}
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="phone">Phone</Label>
        <Input
          id="phone"
          value={values.phone}
          onChange={(e) => setValues((v) => ({ ...v, phone: e.target.value }))}
          placeholder="+2010…"
          required
        />
        {fieldErrors.phone && <p className="text-xs text-destructive">{fieldErrors.phone}</p>}
      </div>

      <div className="space-y-1.5">
        <Label>Payout wallet</Label>
        <p className="text-xs text-muted-foreground">Where we send your payout once a booking completes.</p>
        <div className="flex gap-2">
          <Select
            value={values.walletType}
            onValueChange={(v) => v && setValues((val) => ({ ...val, walletType: v as WalletType }))}
          >
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {(Object.keys(WALLET_LABEL) as WalletType[]).map((type) => (
                <SelectItem key={type} value={type}>
                  {WALLET_LABEL[type]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Input
            value={values.cashbackWalletNumber}
            onChange={(e) => setValues((v) => ({ ...v, cashbackWalletNumber: e.target.value }))}
            placeholder="Wallet number"
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
        Save changes
      </Button>
    </form>
  );
}
