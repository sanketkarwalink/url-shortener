"use client";

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react";

const API = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

type User = { userId: number; email: string; admin: boolean };

type AuthContext = {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<string | null>;
  signup: (email: string, password: string) => Promise<string | null>;
  googleLogin: (credential: string) => Promise<string | null>;
  logout: () => void;
  ready: boolean;
  isAdmin: boolean;
};

const Ctx = createContext<AuthContext>(null!);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [ready, setReady] = useState(false);
  const isAdmin = user?.admin ?? false;

  useEffect(() => {
    const t = localStorage.getItem("token");
    const u = localStorage.getItem("user");
    if (t && u) {
      setToken(t);
      setUser(JSON.parse(u));
    }
    setReady(true);
  }, []);

  const save = (t: string, u: User) => {
    setToken(t);
    setUser(u);
    localStorage.setItem("token", t);
    localStorage.setItem("user", JSON.stringify(u));
  };

  const login = useCallback(async (email: string, password: string): Promise<string | null> => {
    try {
      const res = await fetch(`${API}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({ error: "Login failed" }));
        return err.error || "Login failed";
      }
      const data = await res.json();
      save(data.token, { userId: data.userId, email: data.email, admin: data.admin });
      return null;
    } catch {
      return "Cannot reach server";
    }
  }, []);

  const signup = useCallback(async (email: string, password: string): Promise<string | null> => {
    try {
      const res = await fetch(`${API}/api/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({ error: "Signup failed" }));
        return err.error || "Signup failed";
      }
      const data = await res.json();
      save(data.token, { userId: data.userId, email: data.email, admin: data.admin });
      return null;
    } catch {
      return "Cannot reach server";
    }
  }, []);

  const googleLogin = useCallback(async (credential: string): Promise<string | null> => {
    try {
      const res = await fetch(`${API}/api/auth/google`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ credential }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({ error: "Google sign-in failed" }));
        return err.error || "Google sign-in failed";
      }
      const data = await res.json();
      save(data.token, { userId: data.userId, email: data.email, admin: data.admin });
      return null;
    } catch {
      return "Cannot reach server";
    }
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    localStorage.removeItem("token");
    localStorage.removeItem("user");
  }, []);

  return <Ctx.Provider value={{ user, token, login, signup, googleLogin, logout, ready, isAdmin }}>{children}</Ctx.Provider>;
}

export function useAuth() {
  return useContext(Ctx);
}

export function authHeaders(token: string | null): Record<string, string> {
  if (!token) return {};
  return { Authorization: `Bearer ${token}` };
}
