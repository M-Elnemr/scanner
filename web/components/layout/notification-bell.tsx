import Link from "next/link";
import { Bell } from "lucide-react";
import { apiClient } from "@/lib/auth/server";
import { Button } from "@/components/ui/button";

export async function NotificationBell() {
  const api = await apiClient();
  const result = await api.GET("/api/v1/notifications/me/unread-count", { cache: "no-store" });
  const unread = result.data?.data ?? 0;

  return (
    <Button variant="ghost" size="icon" className="relative" render={<Link href="/notifications" />}>
      <Bell className="size-4.5" />
      {unread > 0 && (
        <span className="absolute right-1.5 top-1.5 flex size-4 items-center justify-center rounded-full bg-gold text-[10px] font-semibold text-gold-foreground">
          {unread > 9 ? "9+" : unread}
        </span>
      )}
    </Button>
  );
}
