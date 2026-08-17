import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin();

const nextConfig: NextConfig = {
  output: "standalone",
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
