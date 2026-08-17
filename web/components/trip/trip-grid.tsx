"use client";

import { motion } from "framer-motion";
import { TripCard } from "@/components/trip/trip-card";
import type { components } from "@/lib/api/schema";

type TripSummary = components["schemas"]["TripSummaryResponse"];

const container = {
  hidden: {},
  show: { transition: { staggerChildren: 0.06 } },
};

const item = {
  hidden: { opacity: 0, y: 16 },
  show: { opacity: 1, y: 0, transition: { duration: 0.35, ease: "easeOut" as const } },
};

export function TripGrid({ trips }: { trips: TripSummary[] }) {
  return (
    <motion.div
      variants={container}
      initial="hidden"
      whileInView="show"
      viewport={{ once: true, margin: "-40px" }}
      className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"
    >
      {trips.map((trip) => (
        <motion.div key={trip.id} variants={item}>
          <TripCard trip={trip} />
        </motion.div>
      ))}
    </motion.div>
  );
}
