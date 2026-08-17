import { requireAdmin } from "@/lib/auth/server";
import { AdminShell } from "@/components/admin/admin-shell";

export default async function AdminLayout({ children }: LayoutProps<"/admin">) {
  // Defense in depth alongside proxy.ts's route guard — this also gives every admin Server
  // Component access to `user` without each of them re-deriving it.
  const user = await requireAdmin();
  return <AdminShell user={user}>{children}</AdminShell>;
}
