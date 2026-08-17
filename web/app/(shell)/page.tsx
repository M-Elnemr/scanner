import Link from "next/link";
import { ArrowRight, BadgeCheck, ShieldCheck, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { HeroSearchCta } from "@/components/home/hero-search-cta";
import { FeaturedTrips } from "@/components/home/featured-trips";

const HIGHLIGHTS = [
  {
    icon: Sparkles,
    title: "Compare with clarity",
    body: "Every package side by side — hotels, inclusions, and price — no digging through PDFs.",
  },
  {
    icon: ShieldCheck,
    title: "Preserve, don't chase",
    body: "Tap once and our client service team reaches out to you — no more cold calls to operators.",
  },
  {
    icon: BadgeCheck,
    title: "Licensed operators only",
    body: "Every company is vetted before its packages ever reach you.",
  },
];

export default function HomePage() {
  return (
    <div>
      <section className="relative overflow-hidden bg-gradient-to-b from-primary/5 via-background to-background">
        <div className="mx-auto flex max-w-6xl flex-col items-center gap-8 px-4 py-20 text-center sm:px-6 sm:py-28">
          <span className="rounded-full border border-primary/20 bg-primary/5 px-4 py-1 text-xs font-medium tracking-wide text-primary uppercase">
            Umrah, planned properly
          </span>
          <h1 className="max-w-3xl font-heading text-4xl font-extrabold tracking-tight text-balance sm:text-6xl">
            Find, compare and preserve your Umrah journey
          </h1>
          <p className="max-w-xl text-lg text-muted-foreground text-pretty">
            Browse verified packages from licensed operators, compare hotels and prices side by
            side, and let us handle the rest.
          </p>
          <HeroSearchCta />
        </div>
      </section>

      <section className="mx-auto grid max-w-6xl gap-6 px-4 py-16 sm:grid-cols-3 sm:px-6">
        {HIGHLIGHTS.map((item) => (
          <div key={item.title} className="rounded-2xl border border-border bg-card p-6">
            <div className="mb-4 flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <item.icon className="size-5" />
            </div>
            <h3 className="font-heading text-lg font-semibold">{item.title}</h3>
            <p className="mt-1 text-sm text-muted-foreground">{item.body}</p>
          </div>
        ))}
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-20 sm:px-6">
        <div className="mb-6 flex items-center justify-between">
          <h2 className="font-heading text-2xl font-bold tracking-tight">Featured journeys</h2>
          <Button variant="ghost" render={<Link href="/trips" />}>
            View all <ArrowRight className="size-4" />
          </Button>
        </div>
        <FeaturedTrips />
      </section>
    </div>
  );
}
