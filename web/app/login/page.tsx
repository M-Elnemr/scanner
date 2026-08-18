import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { GoogleSignInButton } from "@/components/auth/google-sign-in-button";
import { DevSignIn } from "@/components/auth/dev-sign-in";
import { LoginCard } from "@/components/auth/login-card";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("nav");
  return { title: t("signIn") };
}

export default async function LoginPage(props: PageProps<"/login">) {
  const searchParams = await props.searchParams;
  const next = typeof searchParams.next === "string" ? searchParams.next : undefined;

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-gradient-to-br from-primary via-primary to-teal-900 px-4 py-16">
      <div className="pointer-events-none absolute inset-0 opacity-20 [background-image:radial-gradient(circle_at_20%_20%,white,transparent_35%),radial-gradient(circle_at_80%_60%,white,transparent_30%)]" />
      <LoginCard>
        <GoogleSignInButton next={next} />
        {process.env.NODE_ENV !== "production" && <DevSignIn next={next} />}
      </LoginCard>
    </main>
  );
}
