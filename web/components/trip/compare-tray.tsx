"use client";

import { AnimatePresence, motion } from "framer-motion";
import { useRouter } from "next/navigation";
import { Scale, X } from "lucide-react";
import { useCompare } from "@/components/trip/compare-store";
import { Button } from "@/components/ui/button";

export function CompareTray() {
  const { ids, clear, max } = useCompare();
  const router = useRouter();

  return (
    <AnimatePresence>
      {ids.length > 0 && (
        <motion.div
          initial={{ y: 80, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: 80, opacity: 0 }}
          transition={{ type: "spring", stiffness: 300, damping: 30 }}
          className="fixed inset-x-0 bottom-4 z-50 flex justify-center px-4"
        >
          <div className="flex items-center gap-3 rounded-full border border-border bg-card/95 px-4 py-2.5 shadow-xl backdrop-blur">
            <span className="flex items-center gap-1.5 text-sm font-medium">
              <Scale className="size-4 text-primary" />
              {ids.length} of {max} selected
            </span>
            <Button
              size="sm"
              className="rounded-full"
              disabled={ids.length < 2}
              onClick={() => router.push(`/trips/compare?ids=${ids.join(",")}`)}
            >
              Compare
            </Button>
            <Button size="icon-sm" variant="ghost" className="rounded-full" onClick={clear}>
              <X className="size-4" />
            </Button>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
