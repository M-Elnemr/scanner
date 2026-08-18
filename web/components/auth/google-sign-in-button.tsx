"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Script from "next/script";
import { Loader2 } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";

const CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;

export function GoogleSignInButton({ next }: { next?: string }) {
  const buttonRef = useRef<HTMLDivElement>(null);
  const router = useRouter();
  const locale = useLocale();
  const t = useTranslations("login");
  const [scriptReady, setScriptReady] = useState(false);
  const [signingIn, setSigningIn] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!scriptReady || !CLIENT_ID || !buttonRef.current || !window.google) {
      return;
    }

    window.google.accounts.id.initialize({
      client_id: CLIENT_ID,
      ux_mode: "popup",
      callback: async ({ credential }) => {
        setSigningIn(true);
        setError(null);
        try {
          const res = await fetch("/api/auth/google", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ idToken: credential }),
          });
          if (!res.ok) {
            const body = (await res.json().catch(() => null)) as { detail?: string } | null;
            throw new Error(body?.detail ?? t("signInFailed"));
          }
          const { role } = (await res.json()) as { role: "CUSTOMER" | "COMPANY" | "ADMIN" };
          const destination = next && role !== "ADMIN" ? next : role === "ADMIN" ? "/admin" : "/";
          router.push(destination);
          router.refresh();
        } catch (err) {
          setError(err instanceof Error ? err.message : t("signInFailed"));
          setSigningIn(false);
        }
      },
    });

    window.google.accounts.id.renderButton(buttonRef.current, {
      type: "standard",
      theme: "filled_black",
      size: "large",
      text: "signin_with",
      shape: "pill",
      width: 320,
    });
  }, [scriptReady, next, router, t]);

  if (!CLIENT_ID) {
    return <p className="text-sm text-destructive">{t("notConfigured")}</p>;
  }

  return (
    <div className="flex flex-col items-center gap-3">
      <Script
        src={`https://accounts.google.com/gsi/client?hl=${locale}`}
        strategy="afterInteractive"
        onReady={() => setScriptReady(true)}
      />
      <div ref={buttonRef} className="min-h-[44px]" />
      {signingIn && (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="size-4 animate-spin" />
          {t("signingYouIn")}
        </div>
      )}
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
