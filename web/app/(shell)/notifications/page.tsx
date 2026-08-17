import type { Metadata } from "next";
import { apiClient, requireUser } from "@/lib/auth/server";
import { pageableQuery } from "@/lib/api/pageable";
import { NotificationList } from "@/components/notifications/notification-list";

export const metadata: Metadata = { title: "Notifications" };

export default async function NotificationsPage() {
  const user = await requireUser();

  if (user.role !== "CUSTOMER") {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center text-muted-foreground">
        Notifications are available to customer accounts.
      </div>
    );
  }

  const api = await apiClient();
  const result = await api.GET("/api/v1/notifications/me", {
    params: { query: pageableQuery(0, 50, "createdAt,desc") },
    cache: "no-store",
  });
  const notifications = (result.data?.data?.content ?? [])
    .filter((n) => n.id)
    .map((n) => ({
      id: n.id!,
      title: n.title ?? "Notification",
      body: n.body ?? "",
      readAt: n.readAt ?? null,
      createdAt: n.createdAt ?? new Date().toISOString(),
    }));

  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <h1 className="mb-8 font-heading text-3xl font-bold tracking-tight">Notifications</h1>
      <NotificationList notifications={notifications} />
    </div>
  );
}
