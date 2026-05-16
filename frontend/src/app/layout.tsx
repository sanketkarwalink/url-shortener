import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "URL Shortener",
  description: "Shorten URLs and track click analytics",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="min-h-screen">{children}</body>
    </html>
  );
}
