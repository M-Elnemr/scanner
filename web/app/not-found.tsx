import Link from "next/link";
import { Compass } from "lucide-react";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui/button";

export default async function NotFound() {
  const t = await getTranslations("notFound");
  const tErrors = await getTranslations("errors");

  return (
    <div className="mx-auto flex min-h-[70vh] max-w-md flex-col items-center justify-center px-4 text-center">
      <span className="mb-4 flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
        <Compass className="size-6" />
      </span>
      <h1 className="font-heading text-2xl font-bold tracking-tight">{t("title")}</h1>
      <p className="mt-2 text-muted-foreground">{t("description")}</p>
      <Button className="mt-6" render={<Link href="/" />}>
        {tErrors("backToHome")}
      </Button>
    </div>
  );
}
