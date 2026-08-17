"use client";

import { useTransition } from "react";
import { Bell, BellOff } from "lucide-react";
import { markNotificationReadAction } from "@/lib/customer/actions";
import { formatDate } from "@/lib/format/date";
import { cn } from "@/lib/utils";

export interface NotificationItem {
  id: string;
  title: string;
  body: string;
  readAt: string | null;
  createdAt: string;
}

export function NotificationList({ notifications }: { notifications: NotificationItem[] }) {
  const [pending, startTransition] = useTransition();

  if (notifications.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-border py-20 text-center">
        <BellOff className="mx-auto mb-3 size-8 text-muted-foreground" />
        <p className="text-muted-foreground">You&apos;re all caught up — no notifications yet.</p>
      </div>
    );
  }

  return (
    <ul className="space-y-2">
      {notifications.map((n) => (
        <li
          key={n.id}
          className={cn(
            "flex items-start gap-3 rounded-xl border border-border p-4 transition-colors",
            !n.readAt && "bg-primary/5",
          )}
        >
          <span className={cn("mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full", !n.readAt ? "bg-primary/15 text-primary" : "bg-muted text-muted-foreground")}>
            <Bell className="size-4" />
          </span>
          <div className="min-w-0 flex-1">
            <div className="flex items-center justify-between gap-2">
              <p className="font-medium">{n.title}</p>
              <span className="shrink-0 text-xs text-muted-foreground">{formatDate(n.createdAt, "d MMM, HH:mm")}</span>
            </div>
            <p className="mt-0.5 text-sm text-muted-foreground">{n.body}</p>
            {!n.readAt && (
              <button
                type="button"
                disabled={pending}
                onClick={() =>
                  startTransition(async () => {
                    await markNotificationReadAction(n.id);
                  })
                }
                className="mt-2 text-xs font-medium text-primary hover:underline disabled:opacity-60"
              >
                Mark as read
              </button>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}
