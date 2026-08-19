import type { Metadata } from "next";
import { getLocale, getTranslations } from "next-intl/server";
import { formatDate, today } from "@/lib/format/date";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("privacy");
  return { title: t("title") };
}

const CONTACT_EMAIL = "privacy@umrahscan.com";

export default async function PrivacyPolicyPage() {
  const locale = await getLocale();
  const t = await getTranslations("privacy");
  const tCommon = await getTranslations("common");
  const appName = tCommon("appName");
  const sections = t.raw("sections") as { title: string; body: string }[];

  return (
    <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
      <h1 className="font-heading text-3xl font-bold tracking-tight sm:text-4xl">{t("title")}</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        {t("lastUpdated", { date: formatDate(today(), "d MMMM yyyy", locale as "ar" | "en") })}
      </p>
      <p className="mt-6 text-base text-muted-foreground text-pretty">{t("intro", { appName })}</p>

      <div className="mt-10 flex flex-col gap-6">
        {sections.map((section) => (
          <section key={section.title} className="rounded-2xl border border-border bg-card p-6">
            <h2 className="font-heading text-lg font-semibold">{section.title.replace("{appName}", appName)}</h2>
            <p className="mt-2 text-sm leading-relaxed text-muted-foreground text-pretty">
              {section.body.replaceAll("{appName}", appName)}
            </p>
          </section>
        ))}
      </div>

      <section className="mt-10 rounded-2xl border border-border bg-secondary/40 p-6">
        <h2 className="font-heading text-lg font-semibold">{t("contactTitle")}</h2>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground text-pretty">
          {t("contactBody", { email: CONTACT_EMAIL })}
        </p>
      </section>
    </div>
  );
}
