"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { CompanyAddressEditor } from "@/components/admin/company-address-editor";
import { createCompanyAction, updateCompanyAction, type CompanyAddressInput } from "@/lib/admin/companies-actions";
import type { CityOption } from "@/lib/admin/cities";

interface FormState {
  ownerEmail: string;
  companyName: string;
  licenseNumber: string;
  logoUrl: string;
  whatsapp: string;
  description: string;
  commissionPerTraveler: string;
  autoApprove: boolean;
  addresses: CompanyAddressInput[];
}

export function CompanyForm({
  mode,
  companyId,
  cities,
  initial,
}: {
  mode: "create" | "edit";
  companyId?: string;
  cities: CityOption[];
  initial: Partial<FormState>;
}) {
  const router = useRouter();
  const t = useTranslations("admin.companies.form");
  const tCommon = useTranslations("admin.common");
  const [pending, startTransition] = useTransition();
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [form, setForm] = useState<FormState>({
    ownerEmail: initial.ownerEmail ?? "",
    companyName: initial.companyName ?? "",
    licenseNumber: initial.licenseNumber ?? "",
    logoUrl: initial.logoUrl ?? "",
    whatsapp: initial.whatsapp ?? "",
    description: initial.description ?? "",
    commissionPerTraveler: initial.commissionPerTraveler ?? "",
    autoApprove: initial.autoApprove ?? true,
    addresses: initial.addresses ?? [],
  });

  function submit(e: React.FormEvent) {
    e.preventDefault();
    startTransition(async () => {
      const base = {
        companyName: form.companyName,
        licenseNumber: form.licenseNumber,
        logoUrl: form.logoUrl || undefined,
        whatsapp: form.whatsapp || undefined,
        description: form.description || undefined,
        addresses: form.addresses,
      };

      const result =
        mode === "create"
          ? await createCompanyAction({
              ...base,
              ownerEmail: form.ownerEmail,
              commissionPerTraveler: form.commissionPerTraveler ? Number(form.commissionPerTraveler) : undefined,
              autoApprove: form.autoApprove,
            })
          : await updateCompanyAction(companyId!, base);

      if (result.ok) {
        setFieldErrors({});
        toast.success(mode === "create" ? t("createdToast") : t("updatedToast"));
        router.push(mode === "create" && result.data?.id ? `/admin/companies/${result.data.id}` : `/admin/companies`);
        router.refresh();
      } else {
        setFieldErrors(result.fieldErrors ?? {});
        toast.error(result.error ?? tCommon("somethingWentWrong"));
      }
    });
  }

  return (
    <form onSubmit={submit} className="space-y-6">
      {mode === "create" && (
        <div className="space-y-1.5">
          <Label htmlFor="ownerEmail">{t("ownerEmailLabel")}</Label>
          <Input
            id="ownerEmail"
            type="email"
            value={form.ownerEmail}
            onChange={(e) => setForm((f) => ({ ...f, ownerEmail: e.target.value }))}
            required
          />
          {fieldErrors.ownerEmail && <p className="text-xs text-destructive">{fieldErrors.ownerEmail}</p>}
          <p className="text-xs text-muted-foreground">{t("ownerEmailHint")}</p>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor="companyName">{t("nameLabel")}</Label>
          <Input
            id="companyName"
            value={form.companyName}
            onChange={(e) => setForm((f) => ({ ...f, companyName: e.target.value }))}
            required
          />
          {fieldErrors.companyName && <p className="text-xs text-destructive">{fieldErrors.companyName}</p>}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="licenseNumber">{t("licenceLabel")}</Label>
          <Input
            id="licenseNumber"
            value={form.licenseNumber}
            onChange={(e) => setForm((f) => ({ ...f, licenseNumber: e.target.value }))}
            required
          />
          {fieldErrors.licenseNumber && <p className="text-xs text-destructive">{fieldErrors.licenseNumber}</p>}
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor="whatsapp">{t("whatsappLabel")}</Label>
          <Input id="whatsapp" value={form.whatsapp} onChange={(e) => setForm((f) => ({ ...f, whatsapp: e.target.value }))} />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="logoUrl">{t("logoLabel")}</Label>
          <Input id="logoUrl" value={form.logoUrl} onChange={(e) => setForm((f) => ({ ...f, logoUrl: e.target.value }))} />
        </div>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="description">{t("descriptionLabel")}</Label>
        <Textarea
          id="description"
          rows={3}
          value={form.description}
          onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
        />
      </div>

      {mode === "create" && (
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-1.5">
            <Label htmlFor="commission">{t("commissionLabel")}</Label>
            <Input
              id="commission"
              type="number"
              min={0}
              value={form.commissionPerTraveler}
              onChange={(e) => setForm((f) => ({ ...f, commissionPerTraveler: e.target.value }))}
            />
          </div>
          <div className="flex items-center justify-between rounded-lg border border-border px-3">
            <div>
              <Label htmlFor="autoApprove">{t("autoApproveLabel")}</Label>
              <p className="text-xs text-muted-foreground">{t("autoApproveHint")}</p>
            </div>
            <Switch
              id="autoApprove"
              checked={form.autoApprove}
              onCheckedChange={(v) => setForm((f) => ({ ...f, autoApprove: v }))}
            />
          </div>
        </div>
      )}

      <CompanyAddressEditor
        addresses={form.addresses}
        onChange={(addresses) => setForm((f) => ({ ...f, addresses }))}
        cities={cities}
      />

      <Button type="submit" disabled={pending || form.addresses.length === 0}>
        {pending && <Loader2 className="size-4 animate-spin" />}
        {mode === "create" ? t("createSubmit") : t("saveSubmit")}
      </Button>
    </form>
  );
}
