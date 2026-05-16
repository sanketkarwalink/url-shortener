import type { NextConfig } from "next";

const API = process.env.NEXT_PUBLIC_API_URL || "https://url-shortener-upt2.onrender.com";

const nextConfig: NextConfig = {
  async rewrites() {
    return {
      afterFiles: [
        {
          source: "/:shortCode([a-zA-Z0-9]{6})",
          destination: `${API}/:shortCode`,
        },
      ],
    };
  },
};

export default nextConfig;
