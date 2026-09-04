import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin();

const nextConfig: NextConfig = {
  output: "standalone",
  async redirects() {
    // /trips is the site's effective index — the marketing hero/featured-trips
    // homepage is retired from routing (kept on disk, not deleted) in favor of
    // landing straight on the live trip listing. Temporary (307) since this is
    // a reversible product decision, not a permanent URL move.
    return [{ source: "/", destination: "/trips", permanent: false }];
  },
  images: {
    // Company logos and any future trip/hotel photos are served by the backend at /uploads/**.
    // Same origin in production (nginx proxies both under one host), so this pattern only matters
    // for local dev where the API runs on a different port.
    remotePatterns: [
      { protocol: "http", hostname: "localhost", port: "8080", pathname: "/uploads/**" },
      { protocol: "http", hostname: "127.0.0.1", port: "8080", pathname: "/uploads/**" },
    ],
  },
};

export default withNextIntl(nextConfig);
