import { CheckCircle2, Compass, XCircle } from "lucide-react";
import { getTranslations } from "next-intl/server";
import { Badge } from "@/components/ui/badge";
import { simplifyLeadStatus } from "@/lib/leads/status";
import { cn } from "@/lib/utils";

const STYLES: Record<string, string> = {
  preserved: "bg-primary/10 text-primary border-primary/20",
  completed: "bg-emerald-500/10 text-emerald-700 border-emerald-500/20 dark:text-emerald-400",
  cancelled: "bg-destructive/10 text-destructive border-destructive/20",
};

const ICONS = { preserved: Compass, completed: CheckCircle2, cancelled: XCircle };

export async function LeadStatusBadge({ status }: { status: string | undefined }) {
  const state = simplifyLeadStatus(status);
  const Icon = ICONS[state];
  const t = await getTranslations("leadStatus");
  return (
    <Badge variant="outline" className={cn("gap-1 rounded-full", STYLES[state])}>
      <Icon className="size-3" />
      {t(state)}
    </Badge>
  );
}
