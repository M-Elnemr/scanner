"use client";

import { useRouter, usePathname, useSearchParams } from "next/navigation";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

export function TablePagination({ page, totalPages }: { page: number; totalPages: number }) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  if (totalPages <= 1) return null;

  function goTo(targetPage: number) {
    const params = new URLSearchParams(searchParams.toString());
    params.set("page", String(targetPage));
    router.push(`${pathname}?${params.toString()}`);
  }

  return (
    <div className="flex items-center justify-center gap-2">
      <Button variant="outline" size="icon" disabled={page <= 0} onClick={() => goTo(page - 1)}>
        <ChevronLeft className="size-4" />
      </Button>
      <span className="px-3 text-sm text-muted-foreground">
        Page {page + 1} of {totalPages}
      </span>
      <Button variant="outline" size="icon" disabled={page >= totalPages - 1} onClick={() => goTo(page + 1)}>
        <ChevronRight className="size-4" />
      </Button>
    </div>
  );
}
