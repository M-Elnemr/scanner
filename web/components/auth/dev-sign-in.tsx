"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";

/** Only rendered when NODE_ENV !== "production" — see app/api/auth/dev-login/route.ts. */
export function DevSignIn({ next }: { next?: string }) {
  const router = useRouter();
  const t = useTranslations("login");
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<"CUSTOMER" | "ADMIN">("CUSTOMER");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const res = await fetch("/api/auth/dev-login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, role }),
      });
      if (!res.ok) {
        const body = (await res.json().catch(() => null)) as { detail?: string } | null;
        throw new Error(body?.detail ?? t("signInFailed"));
      }
      router.push(role === "ADMIN" ? "/admin" : (next ?? "/"));
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : t("signInFailed"));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="w-full space-y-3">
      <div className="flex items-center gap-2">
        <Separator className="flex-1" />
        <span className="text-xs text-muted-foreground">{t("devOnly")}</span>
        <Separator className="flex-1" />
      </div>
      <form onSubmit={submit} className="flex flex-col gap-2">
        <Input
          type="email"
          placeholder={t("devEmailPlaceholder")}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <div className="flex gap-2">
          <Select
            value={role}
            onValueChange={(v) => setRole(v as "CUSTOMER" | "ADMIN")}
            items={[
              { value: "CUSTOMER", label: t("roleCustomer") },
              { value: "ADMIN", label: t("roleAdmin") },
            ]}
          >
            <SelectTrigger className="w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="CUSTOMER">{t("roleCustomer")}</SelectItem>
              <SelectItem value="ADMIN">{t("roleAdmin")}</SelectItem>
            </SelectContent>
          </Select>
          <Button type="submit" variant="secondary" className="flex-1" disabled={loading}>
            {loading ? t("signingIn") : t("devSignIn")}
          </Button>
        </div>
        {error && <p className="text-xs text-destructive">{error}</p>}
      </form>
    </div>
  );
}
