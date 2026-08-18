import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui/button";
import { ChevronLeft, ChevronRight } from "lucide-react";

export async function TripPagination({
  page,
  totalPages,
  basePath,
  searchParams,
}: {
  page: number;
  totalPages: number;
  basePath: string;
  searchParams: URLSearchParams;
}) {
  if (totalPages <= 1) return null;
  const t = await getTranslations("trips");

  function hrefFor(targetPage: number) {
    const params = new URLSearchParams(searchParams);
    params.set("page", String(targetPage));
    return `${basePath}?${params.toString()}`;
  }

  const hasPrev = page > 0;
  const hasNext = page < totalPages - 1;

  return (
    <div className="mt-8 flex items-center justify-center gap-2">
      {hasPrev ? (
        <Button variant="outline" size="icon" render={<Link href={hrefFor(page - 1)} />}>
          <ChevronLeft className="size-4" />
        </Button>
      ) : (
        <Button variant="outline" size="icon" disabled>
          <ChevronLeft className="size-4" />
        </Button>
      )}
      <span className="px-3 text-sm text-muted-foreground">
        {t("pageOf", { page: page + 1, totalPages })}
      </span>
      {hasNext ? (
        <Button variant="outline" size="icon" render={<Link href={hrefFor(page + 1)} />}>
          <ChevronRight className="size-4" />
        </Button>
      ) : (
        <Button variant="outline" size="icon" disabled>
          <ChevronRight className="size-4" />
        </Button>
      )}
    </div>
  );
}
