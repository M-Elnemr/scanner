"use client";

import { useState } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import { Search, X } from "lucide-react";
import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

export function AdminFilterBar({
  statusOptions,
  companyOptions,
  searchPlaceholder,
}: {
  statusOptions: { value: string; label: string }[];
  companyOptions?: { value: string; label: string }[];
  searchPlaceholder?: string;
}) {
  const t = useTranslations("admin.common");
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [search, setSearch] = useState(searchParams.get("search") ?? "");
  const status = searchParams.get("status") ?? "";
  const companyId = searchParams.get("companyId") ?? "";

  function pushParams(mutate: (params: URLSearchParams) => void) {
    const params = new URLSearchParams(searchParams.toString());
    mutate(params);
    params.delete("page");
    router.push(`${pathname}?${params.toString()}`);
  }

  function submitSearch(e: React.FormEvent) {
    e.preventDefault();
    pushParams((params) => {
      if (search.trim()) params.set("search", search.trim());
      else params.delete("search");
    });
  }

  const hasFilters = Boolean(status || companyId || searchParams.get("search"));

  return (
    <div className="flex flex-wrap items-center gap-2">
      <form onSubmit={submitSearch} className="relative">
        <Search className="pointer-events-none absolute top-1/2 left-2.5 size-3.5 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={searchPlaceholder ?? t("searchPlaceholderDefault")}
          className="w-64 pl-8"
        />
      </form>

      <Select
        value={status}
        onValueChange={(v) => pushParams((params) => (v ? params.set("status", v) : params.delete("status")))}
      >
        <SelectTrigger className="w-44">
          <SelectValue placeholder={t("allStatuses")} />
        </SelectTrigger>
        <SelectContent>
          {statusOptions.map((o) => (
            <SelectItem key={o.value} value={o.value}>
              {o.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {companyOptions && (
        <Select
          value={companyId}
          onValueChange={(v) => pushParams((params) => (v ? params.set("companyId", v) : params.delete("companyId")))}
        >
          <SelectTrigger className="w-44">
            <SelectValue placeholder={t("allCompanies")} />
          </SelectTrigger>
          <SelectContent>
            {companyOptions.map((o) => (
              <SelectItem key={o.value} value={o.value}>
                {o.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      )}

      {hasFilters && (
        <Button
          variant="ghost"
          size="sm"
          className="text-muted-foreground"
          onClick={() => {
            setSearch("");
            router.push(pathname);
          }}
        >
          <X className="size-3.5" /> {t("clear")}
        </Button>
      )}
    </div>
  );
}
