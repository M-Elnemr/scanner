import Link from "next/link";
import { getTranslations } from "next-intl/server";

export async function SiteFooter() {
  const t = await getTranslations();

  return (
    <footer className="mt-auto border-t border-border/60 bg-secondary/40">
      <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-10 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between sm:px-6">
        <p>{t("footer.rights", { year: new Date().getFullYear(), appName: t("common.appName") })}</p>
        <div className="flex gap-6">
          <Link href="/trips" className="hover:text-foreground">
            {t("nav.browseTrips")}
          </Link>
          <Link href="/login" className="hover:text-foreground">
            {t("nav.signIn")}
          </Link>
          <Link href="/privacy" className="hover:text-foreground">
            {t("footer.privacy")}
          </Link>
        </div>
      </div>
    </footer>
  );
}
