"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/lib/auth";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { GoogleLogin } from "@react-oauth/google";

export default function LoginPage() {
  const { login, googleLogin, user, ready } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (ready && user) router.push("/");
  }, [ready, user, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    const err = await login(email, password);
    if (err) setError(err);
    else router.push("/");
    setLoading(false);
  };

  if (!ready) return null;

  return (
    <div className="min-h-screen flex items-center justify-center px-5">
      <div className="w-full max-w-sm">
        <div className="text-center mb-10">
          <h1 className="text-3xl font-bold tracking-tight mb-2">Welcome back</h1>
          <p className="text-sm text-[var(--muted)]">Sign in to your account</p>
        </div>

        <div className="flex justify-center mb-6">
          <GoogleLogin
            onSuccess={async (response) => {
              const err = await googleLogin(response.credential!);
              if (err) setError(err);
              else router.push("/");
            }}
            onError={() => setError("Google sign-in failed")}
            theme="outline"
            size="large"
            text="signin_with"
            shape="rectangular"
          />
        </div>

        <div className="flex items-center gap-3 mb-6">
          <div className="flex-1 h-px bg-[var(--border)]" />
          <span className="text-xs text-[var(--muted-light)]">OR</span>
          <div className="flex-1 h-px bg-[var(--border)]" />
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Email"
              required
              className="w-full px-4 py-3 rounded-xl bg-[var(--surface)] border border-[var(--border)] text-sm outline-none focus:border-[var(--accent-light)] transition-colors"
            />
          </div>
          <div>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Password"
              required
              className="w-full px-4 py-3 rounded-xl bg-[var(--surface)] border border-[var(--border)] text-sm outline-none focus:border-[var(--accent-light)] transition-colors"
            />
          </div>

          {error && (
            <p className="text-sm text-red-500 text-center">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 rounded-xl text-sm font-medium disabled:opacity-50 cursor-pointer"
            style={{
              background: "linear-gradient(135deg, var(--fg), var(--accent-light))",
              color: "var(--bg)",
            }}
          >
            {loading ? "Signing in..." : "Sign in"}
          </button>
        </form>

        <p className="text-center text-sm text-[var(--muted)] mt-6">
          Don&apos;t have an account?{" "}
          <Link href="/signup" className="text-[var(--accent-light)] hover:underline">
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
}
