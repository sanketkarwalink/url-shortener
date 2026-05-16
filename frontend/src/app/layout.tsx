import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "@/lib/auth";
import { GoogleOAuthProvider } from "@react-oauth/google";

export const metadata: Metadata = {
  title: "URL Shortener",
  description: "Shorten URLs and track click analytics",
};

const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || "";

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="min-h-screen">
        <GoogleOAuthProvider clientId={googleClientId}>
          <AuthProvider>{children}</AuthProvider>
        </GoogleOAuthProvider>
      </body>
    </html>
  );
}
