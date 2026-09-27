import type { NextConfig } from "next";

// Static export: `npm run build` writes plain HTML to ./out, which Cloudflare Pages serves as-is.
const nextConfig: NextConfig = {
  output: "export",
  trailingSlash: true,
  images: { unoptimized: true },
};

export default nextConfig;
