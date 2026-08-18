"use client";

import { useState, useTransition } from "react";
import { toast } from "sonner";
import { Loader2, MessageCircle, ExternalLink, Building2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { composeCompanyWhatsAppAction, composeTripWhatsAppAction, type WhatsAppMessage } from "@/lib/admin/whatsapp-actions";

type Kind = "trip" | "company";

export function WhatsAppButtons({ leadId, compact = false }: { leadId: string; compact?: boolean }) {
  const t = useTranslations("admin.whatsapp");
  const [pending, startTransition] = useTransition();
  const [open, setOpen] = useState(false);
  const [lang, setLang] = useState<"ar" | "en">("ar");
  const [kind, setKind] = useState<Kind>("trip");
  const [preview, setPreview] = useState<WhatsAppMessage | null>(null);

  function compose(nextKind: Kind, nextLang: "ar" | "en" = lang) {
    setKind(nextKind);
    setLang(nextLang);
    startTransition(async () => {
      const result =
        nextKind === "trip"
          ? await composeTripWhatsAppAction(leadId, nextLang)
          : await composeCompanyWhatsAppAction(leadId, nextLang);
      if (result.ok) {
        setPreview(result.data);
        setOpen(true);
      } else {
        toast.error(result.error);
      }
    });
  }

  return (
    <>
      {compact ? (
        <div className="flex items-center gap-1">
          <Tooltip>
            <TooltipTrigger
              render={
                <Button
                  variant="ghost"
                  size="icon"
                  disabled={pending}
                  onClick={() => compose("trip")}
                  aria-label={t("messageAboutTrip")}
                />
              }
            >
              {pending && kind === "trip" ? <Loader2 className="size-4 animate-spin" /> : <MessageCircle className="size-4" />}
            </TooltipTrigger>
            <TooltipContent>{t("messageAboutTrip")}</TooltipContent>
          </Tooltip>
          <Tooltip>
            <TooltipTrigger
              render={
                <Button
                  variant="ghost"
                  size="icon"
                  disabled={pending}
                  onClick={() => compose("company")}
                  aria-label={t("messageAboutCompany")}
                />
              }
            >
              {pending && kind === "company" ? <Loader2 className="size-4 animate-spin" /> : <Building2 className="size-4" />}
            </TooltipTrigger>
            <TooltipContent>{t("messageAboutCompany")}</TooltipContent>
          </Tooltip>
        </div>
      ) : (
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" disabled={pending} onClick={() => compose("trip")}>
            {pending && kind === "trip" ? <Loader2 className="size-4 animate-spin" /> : <MessageCircle className="size-4" />}
            {t("messageAboutTrip")}
          </Button>
          <Button variant="outline" disabled={pending} onClick={() => compose("company")}>
            {pending && kind === "company" ? <Loader2 className="size-4 animate-spin" /> : <MessageCircle className="size-4" />}
            {t("messageAboutCompany")}
          </Button>
        </div>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t("previewTitle")}</DialogTitle>
            <DialogDescription>
              {t("previewDesc", {
                name: preview?.recipientName ?? t("defaultRecipient"),
                phone: preview?.recipientPhone ?? "—",
              })}
            </DialogDescription>
          </DialogHeader>

          <Select value={lang} onValueChange={(v) => v && compose(kind, v as "ar" | "en")}>
            <SelectTrigger className="w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ar">{t("languageArabic")}</SelectItem>
              <SelectItem value="en">{t("languageEnglish")}</SelectItem>
            </SelectContent>
          </Select>

          <Textarea readOnly rows={8} value={preview?.message ?? ""} dir={lang === "ar" ? "rtl" : "ltr"} />

          <DialogFooter>
            <Button className="w-full" disabled={!preview?.link} render={<a href={preview?.link} target="_blank" rel="noreferrer" />}>
              <ExternalLink className="size-4" /> {t("openInWhatsapp")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
