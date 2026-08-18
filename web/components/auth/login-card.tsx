"use client";

import { motion } from "framer-motion";
import { Compass } from "lucide-react";
import { useTranslations } from "next-intl";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

export function LoginCard({ children }: { children: React.ReactNode }) {
  const t = useTranslations();

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: "easeOut" }}
      className="relative z-10 w-full max-w-md"
    >
      <Card className="border-white/10 bg-card/95 shadow-2xl backdrop-blur">
        <CardHeader className="items-center gap-3 pb-2 text-center">
          <div className="flex size-14 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <Compass className="size-7" />
          </div>
          <div className="space-y-1">
            <h1 className="font-heading text-2xl font-bold tracking-tight">{t("common.appName")}</h1>
            <p className="text-sm text-muted-foreground">{t("login.subtitle")}</p>
          </div>
        </CardHeader>
        <CardContent className="flex flex-col items-center gap-6 pt-4 pb-8">
          {children}
          <p className="text-center text-xs text-muted-foreground">{t("login.disclaimer")}</p>
        </CardContent>
      </Card>
    </motion.div>
  );
}
