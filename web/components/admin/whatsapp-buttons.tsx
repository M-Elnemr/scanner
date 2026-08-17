"use client";

import { useState, useTransition } from "react";
import { toast } from "sonner";
import { Loader2, MessageCircle, ExternalLink } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { composeCompanyWhatsAppAction, composeTripWhatsAppAction, type WhatsAppMessage } from "@/lib/admin/whatsapp-actions";

type Kind = "trip" | "company";

export function WhatsAppButtons({ leadId }: { leadId: string }) {
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
      <div className="flex flex-wrap gap-2">
        <Button variant="outline" disabled={pending} onClick={() => compose("trip")}>
          {pending && kind === "trip" ? <Loader2 className="size-4 animate-spin" /> : <MessageCircle className="size-4" />}
          Message about trip
        </Button>
        <Button variant="outline" disabled={pending} onClick={() => compose("company")}>
          {pending && kind === "company" ? <Loader2 className="size-4 animate-spin" /> : <MessageCircle className="size-4" />}
          Message about company
        </Button>
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>WhatsApp message preview</DialogTitle>
            <DialogDescription>
              To {preview?.recipientName ?? "the customer"} ({preview?.recipientPhone ?? "—"}). Nothing is sent until
              you press send in WhatsApp.
            </DialogDescription>
          </DialogHeader>

          <Select value={lang} onValueChange={(v) => v && compose(kind, v as "ar" | "en")}>
            <SelectTrigger className="w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ar">Arabic</SelectItem>
              <SelectItem value="en">English</SelectItem>
            </SelectContent>
          </Select>

          <Textarea readOnly rows={8} value={preview?.message ?? ""} dir={lang === "ar" ? "rtl" : "ltr"} />

          <DialogFooter>
            <Button className="w-full" disabled={!preview?.link} render={<a href={preview?.link} target="_blank" rel="noreferrer" />}>
              <ExternalLink className="size-4" /> Open in WhatsApp
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
