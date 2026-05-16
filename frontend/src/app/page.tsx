"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuth, authHeaders } from "@/lib/auth";
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend,
} from "recharts";

const API = process.env.NEXT_PUBLIC_API_URL || "https://url-shortener-upt2.onrender.com";

type UrlEntry = {
  id: number;
  originalUrl: string;
  shortCode: string;
  shortUrl: string;
  createdAt: string;
  clickCount: number;
};

type Analytics = {
  shortCode: string;
  originalUrl: string;
  totalClicks: number;
  dailyClicks: { date: string; count: number }[];
  devices: { label: string; count: number }[];
  browsers: { label: string; count: number }[];
  referrers: { label: string; count: number }[];
};

type PageResponse = {
  content: UrlEntry[];
  totalPages: number;
  totalElements: number;
  number: number;
  first: boolean;
  last: boolean;
};

const COLORS = ["#6366f1", "#8b5cf6", "#a78bfa", "#c4b5fd", "#10b981", "#f59e0b", "#ef4444"];
const PIE_COLORS = ["#6366f1", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6"];

function timeAgo(dateStr: string): string {
  const ms = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(ms / 60000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

function Toast({ message, type }: { message: string; type: "success" | "error" }) {
  return (
    <div className="fixed bottom-6 right-6 z-50 animate-slide-up">
      <div
        className={`flex items-center gap-2.5 px-5 py-3 rounded-2xl shadow-lg text-sm font-medium backdrop-blur-xl ${
          type === "success"
            ? "bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800"
            : "bg-red-50 dark:bg-red-950/40 text-red-700 dark:text-red-300 border border-red-200 dark:border-red-800"
        }`}
      >
        <span className="text-base">{type === "success" ? "✓" : "✕"}</span>
        {message}
      </div>
    </div>
  );
}

export default function Home() {
  const { user, token, logout, ready } = useAuth();
  const router = useRouter();

  const [urls, setUrls] = useState<UrlEntry[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [url, setUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(false);
  const [copyCode, setCopyCode] = useState<string | null>(null);
  const [toast, setToast] = useState<{ message: string; type: "success" | "error" } | null>(null);
  const [deleting, setDeleting] = useState<number | null>(null);

  const showToast = (message: string, type: "success" | "error") => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 2500);
  };

  const fetchUrls = useCallback(async (p: number) => {
    try {
      const res = await fetch(`${API}/api/urls?page=${p}&size=50`, {
        headers: { ...authHeaders(token) },
      });
      if (res.ok) {
        const data: PageResponse = await res.json();
        setUrls(data.content);
        setPage(data.number);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      }
    } catch {}
  }, [token]);

  useEffect(() => {
    if (ready && user) fetchUrls(0);
  }, [ready, user, fetchUrls]);

  const createUrl = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!url.trim()) return;
    if (!user) { router.push("/signup"); return; }
    setLoading(true);
    try {
      const res = await fetch(`${API}/api/urls`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeaders(token) },
        body: JSON.stringify({ originalUrl: url.trim() }),
      });
      if (res.ok) {
        const data = await res.json();
        setUrl("");
        await fetchUrls(0);
        showToast("URL shortened! Ready to share.", "success");
        navigator.clipboard.writeText(data.shortUrl).then(() => {
          showToast("Copied to clipboard!", "success");
        }).catch(() => {});
      } else {
        const err = await res.json().catch(() => ({ error: "Invalid URL" }));
        showToast(err.error || "Failed to create short URL", "error");
      }
    } catch {
      showToast("Cannot reach server. Is the backend running on port 8080?", "error");
    } finally {
      setLoading(false);
    }
  };

  const viewAnalytics = async (entry: UrlEntry) => {
    setAnalyticsLoading(true);
    setAnalytics(null);
    try {
      const res = await fetch(`${API}/api/urls/${entry.id}/analytics`, {
        headers: { ...authHeaders(token) },
      });
      if (res.ok) setAnalytics(await res.json());
    } catch {}
    setAnalyticsLoading(false);
  };

  const deleteUrl = async (id: number) => {
    setDeleting(id);
    try {
      await fetch(`${API}/api/urls/${id}`, {
        method: "DELETE",
        headers: { ...authHeaders(token) },
      });
      setAnalytics(null);
      await fetchUrls(page);
      showToast("URL deleted", "success");
    } catch {}
    setDeleting(null);
  };

  const copyToClipboard = (text: string, code: string) => {
    navigator.clipboard.writeText(text);
    setCopyCode(code);
    showToast("Copied to clipboard!", "success");
    setTimeout(() => setCopyCode(null), 2000);
  };

  if (!ready) return null;

  return (
    <div className="min-h-screen">
      <div className="max-w-4xl mx-auto px-5 py-12 md:py-20">
        {/* Top bar */}
        <div className="flex items-center justify-between mb-8">
          {user ? (
            <>
              <span className="text-sm text-[var(--muted)]">{user.email}</span>
              <button onClick={logout} className="text-sm text-[var(--muted-light)] hover:text-[var(--fg)] transition-colors cursor-pointer">
                Sign out
              </button>
            </>
          ) : (
            <>
              <span />
              <div className="flex items-center gap-3">
                <a href="/login" className="text-sm text-[var(--muted)] hover:text-[var(--fg)] transition-colors">Sign in</a>
                <a href="/signup" className="text-sm font-medium px-3 py-1.5 rounded-lg border border-[var(--border)] hover:border-[var(--fg)]/30 transition-all">
                  Sign up
                </a>
              </div>
            </>
          )}
        </div>

        {/* Hero */}
        <div className="text-center mb-12 animate-fade-in">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[var(--surface)] border border-[var(--border)] text-xs text-[var(--muted)] mb-6">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
            Free & open source
          </div>
          <h1 className="text-5xl md:text-7xl font-bold tracking-tight leading-[0.95] mb-4">
            <span className="bg-gradient-to-r from-[var(--fg)] via-[var(--accent-light)] to-[var(--fg)] bg-clip-text text-transparent">
              Shorten
            </span>
            <span className="text-[var(--muted)]">.</span>
            <br />
            <span className="text-[var(--muted)]">Track.</span>
            <span className="text-[var(--fg)]"> Analyze.</span>
          </h1>
          <p className="text-base md:text-lg text-[var(--muted)] max-w-md mx-auto leading-relaxed">
            Create short links and see exactly who clicks them — all self-hosted.
          </p>
        </div>

        {/* Form */}
        <form onSubmit={createUrl} className="animate-slide-up mb-12">
          <div className="relative group">
            <div className="absolute -inset-0.5 bg-gradient-to-r from-[var(--accent-light)] to-[var(--accent-light)] opacity-20 group-hover:opacity-30 blur-xl rounded-2xl transition-all duration-500" />
            <div className="relative flex gap-2 p-1.5 rounded-2xl bg-[var(--surface)] border border-[var(--border)] shadow-sm">
              <input
                type="url"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                placeholder="Paste your long URL here..."
                required
                className="flex-1 px-5 py-3.5 rounded-xl bg-transparent text-sm outline-none placeholder:text-[var(--muted-light)] focus:outline-none"
              />
              <button
                type="submit"
                disabled={loading}
                className="px-6 py-3.5 rounded-xl text-sm font-medium transition-all duration-200 disabled:opacity-50 flex items-center gap-2 cursor-pointer"
                style={{
                  background: "linear-gradient(135deg, var(--fg), var(--accent-light))",
                  color: "var(--bg)",
                }}
              >
                {loading ? (
                  <span className="flex items-center gap-2">
                    <svg className="animate-spin w-4 h-4" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                    Shortening...
                  </span>
                ) : (
                  <span className="flex items-center gap-2">
                    Shorten
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                    </svg>
                  </span>
                )}
              </button>
            </div>
          </div>
        </form>

        {/* Stats bar */}
        {totalElements > 0 && (
          <div className="animate-fade-in flex items-center gap-6 mb-6 text-sm text-[var(--muted)] justify-center flex-wrap">
            <span className="flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-[var(--accent-light)]" />
              {totalElements} link{totalElements !== 1 ? "s" : ""}
            </span>
            <span className="w-1 h-1 rounded-full bg-[var(--border)]" />
            <span className="flex items-center gap-1.5">
              Page {page + 1} of {Math.max(totalPages, 1)}
            </span>
          </div>
        )}

        {/* URL list */}
        {urls.length === 0 && totalElements === 0 ? (
          <div className="text-center py-16 animate-fade-in">
            <div className="w-16 h-16 mx-auto mb-6 rounded-2xl bg-[var(--surface)] border border-[var(--border)] flex items-center justify-center">
              <svg className="w-7 h-7 text-[var(--muted-light)]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
              </svg>
            </div>
            <h3 className="text-lg font-medium mb-2">No links yet</h3>
            <p className="text-sm text-[var(--muted)] max-w-xs mx-auto">
              Paste a URL above to create your first short link.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {urls.map((entry, i) => (
              <div
                key={entry.id}
                className="animate-slide-up group relative bg-[var(--surface)] border border-[var(--border)] rounded-2xl p-4 md:p-5 hover:border-[var(--muted-light)]/40 transition-all duration-300"
                style={{ animationDelay: `${i * 50}ms`, animationFillMode: "both" }}
              >
                <div className="flex items-start gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-3 mb-1.5">
                      <span className="font-mono text-sm font-semibold bg-gradient-to-r from-[var(--accent-light)] to-[var(--fg)] bg-clip-text text-transparent">
                        {entry.shortCode}
                      </span>
                      <span className="text-xs text-[var(--muted-light)]">{timeAgo(entry.createdAt)}</span>
                      <span className="text-xs px-2 py-0.5 rounded-full bg-[var(--surface)] border border-[var(--border)]">
                        {entry.clickCount} click{entry.clickCount !== 1 ? "s" : ""}
                      </span>
                    </div>
                    <p className="text-sm text-[var(--muted)] truncate max-w-lg">
                      {entry.originalUrl}
                    </p>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      onClick={() => copyToClipboard(entry.shortUrl, entry.shortCode)}
                      className={`px-3.5 py-2 rounded-xl text-xs font-medium border transition-all duration-200 ${
                        copyCode === entry.shortCode
                          ? "bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800 text-emerald-600 dark:text-emerald-400"
                          : "border-[var(--border)] hover:border-[var(--fg)]/30 hover:bg-[var(--bg)]"
                      }`}
                    >
                      {copyCode === entry.shortCode ? "Copied!" : "Copy"}
                    </button>
                    <button
                      onClick={() => viewAnalytics(entry)}
                      className="px-3.5 py-2 rounded-xl text-xs font-medium border border-[var(--border)] hover:border-[var(--fg)]/30 hover:bg-[var(--bg)] transition-all duration-200"
                    >
                      Analytics
                    </button>
                    <button
                      onClick={() => deleteUrl(entry.id)}
                      disabled={deleting === entry.id}
                      className="px-3.5 py-2 rounded-xl text-xs font-medium border border-transparent text-[var(--muted-light)] hover:text-[var(--error)] hover:border-red-200 dark:hover:border-red-900 hover:bg-red-50 dark:hover:bg-red-950/30 transition-all duration-200 disabled:opacity-30"
                    >
                      {deleting === entry.id ? "..." : "Delete"}
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-4 mt-8 animate-fade-in">
            <button
              disabled={page === 0}
              onClick={() => fetchUrls(page - 1)}
              className="px-5 py-2.5 rounded-xl text-sm font-medium border border-[var(--border)] disabled:opacity-30 hover:border-[var(--fg)]/30 transition-all duration-200"
            >
              ← Previous
            </button>
            <div className="flex items-center gap-2">
              {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                const p = Math.max(0, Math.min(page - 2, totalPages - 5));
                const pageNum = p + i;
                if (pageNum >= totalPages) return null;
                return (
                  <button
                    key={pageNum}
                    onClick={() => fetchUrls(pageNum)}
                    className={`w-9 h-9 rounded-xl text-sm font-medium transition-all duration-200 ${
                      pageNum === page
                        ? "bg-[var(--fg)] text-[var(--bg)]"
                        : "border border-[var(--border)] hover:border-[var(--fg)]/30"
                    }`}
                  >
                    {pageNum + 1}
                  </button>
                );
              })}
            </div>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => fetchUrls(page + 1)}
              className="px-5 py-2.5 rounded-xl text-sm font-medium border border-[var(--border)] disabled:opacity-30 hover:border-[var(--fg)]/30 transition-all duration-200"
            >
              Next →
            </button>
          </div>
        )}
      </div>

      {/* Analytics Modal */}
      {analytics && (
        <div className="fixed inset-0 z-50 flex items-start justify-center pt-8 md:pt-16 pb-8 overflow-auto">
          <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" onClick={() => setAnalytics(null)} />
          <div className="relative w-full max-w-3xl mx-4 animate-slide-up">
            <div className="bg-[var(--bg)] border border-[var(--border)] rounded-3xl shadow-2xl overflow-hidden">
              {/* Header */}
              <div className="px-6 md:px-8 pt-6 md:pt-8 pb-6 border-b border-[var(--border)]">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h2 className="text-xl font-bold">Analytics</h2>
                      <span className="px-2.5 py-0.5 rounded-full bg-[var(--surface)] border border-[var(--border)] text-xs font-mono text-[var(--accent-light)]">
                        {analytics.shortCode}
                      </span>
                    </div>
                    <p className="text-sm text-[var(--muted)] truncate max-w-lg">
                      {analytics.originalUrl}
                    </p>
                  </div>
                  <div className="flex items-center gap-4 shrink-0">
                    <div className="text-right">
                      <p className="text-3xl font-bold">{analytics.totalClicks}</p>
                      <p className="text-xs text-[var(--muted)]">total clicks</p>
                    </div>
                    <button
                      onClick={() => setAnalytics(null)}
                      className="w-8 h-8 rounded-xl border border-[var(--border)] flex items-center justify-center text-sm hover:bg-[var(--surface)] transition-colors"
                    >
                      ✕
                    </button>
                  </div>
                </div>
              </div>

              {analytics.totalClicks === 0 ? (
                <div className="px-8 py-16 text-center">
                  <div className="w-14 h-14 mx-auto mb-4 rounded-2xl bg-[var(--surface)] border border-[var(--border)] flex items-center justify-center">
                    <svg className="w-6 h-6 text-[var(--muted-light)]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M11 3.055A9.001 9.001 0 1020.945 13H11V3.055z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20.488 9H15V3.512A9.025 9.025 0 0120.488 9z" />
                    </svg>
                  </div>
                  <p className="text-sm text-[var(--muted)]">No clicks yet. Share the link to start collecting data.</p>
                </div>
              ) : (
                <div className="px-6 md:px-8 py-6 space-y-8">
                  {analytics.dailyClicks.length > 0 && (
                    <div>
                      <p className="text-sm font-semibold mb-4">Last 30 Days</p>
                      <div className="h-48 md:h-56">
                        <ResponsiveContainer width="100%" height="100%">
                          <BarChart data={analytics.dailyClicks} barCategoryGap="20%">
                            <XAxis dataKey="date" tick={{ fontSize: 11, fill: "var(--muted)" }} tickFormatter={(v) => v.slice(5)} axisLine={false} tickLine={false} />
                            <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: "var(--muted)" }} axisLine={false} tickLine={false} />
                            <Tooltip
                              contentStyle={{
                                background: "var(--surface)",
                                border: "1px solid var(--border)",
                                borderRadius: "12px",
                                fontSize: "13px",
                              }}
                            />
                            <Bar dataKey="count" fill="var(--accent-light)" radius={[6, 6, 0, 0]} />
                          </BarChart>
                        </ResponsiveContainer>
                      </div>
                    </div>
                  )}

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {analytics.devices.length > 0 && (
                      <div className="p-5 rounded-2xl bg-[var(--surface)] border border-[var(--border)]">
                        <p className="text-sm font-semibold mb-3">Devices</p>
                        <div className="h-44">
                          <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                              <Pie data={analytics.devices} dataKey="count" nameKey="label" cx="50%" cy="50%" outerRadius={60}>
                                {analytics.devices.map((_, i) => (
                                  <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                                ))}
                              </Pie>
                              <Tooltip contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: "12px", fontSize: "13px" }} />
                              <Legend wrapperStyle={{ fontSize: "11px" }} />
                            </PieChart>
                          </ResponsiveContainer>
                        </div>
                      </div>
                    )}

                    {analytics.browsers.length > 0 && (
                      <div className="p-5 rounded-2xl bg-[var(--surface)] border border-[var(--border)]">
                        <p className="text-sm font-semibold mb-3">Browsers</p>
                        <div className="h-44">
                          <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                              <Pie data={analytics.browsers} dataKey="count" nameKey="label" cx="50%" cy="50%" outerRadius={60}>
                                {analytics.browsers.map((_, i) => (
                                  <Cell key={i} fill={PIE_COLORS[(i + 2) % PIE_COLORS.length]} />
                                ))}
                              </Pie>
                              <Tooltip contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: "12px", fontSize: "13px" }} />
                              <Legend wrapperStyle={{ fontSize: "11px" }} />
                            </PieChart>
                          </ResponsiveContainer>
                        </div>
                      </div>
                    )}
                  </div>

                  {analytics.referrers.length > 0 && (
                    <div>
                      <p className="text-sm font-semibold mb-4">Top Referrers</p>
                      <div className="space-y-2.5">
                        {analytics.referrers.map((r, i) => {
                          const maxCount = Math.max(...analytics.referrers.map((x) => x.count));
                          return (
                            <div key={i} className="flex items-center gap-4">
                              <span className="w-32 text-sm text-[var(--muted)] truncate shrink-0">
                                {r.label || "(direct)"}
                              </span>
                              <div className="flex-1 h-2 rounded-full bg-[var(--border)] overflow-hidden">
                                <div
                                  className="h-full rounded-full transition-all duration-500"
                                  style={{
                                    width: `${(r.count / maxCount) * 100}%`,
                                    background: "linear-gradient(90deg, var(--accent-light), var(--fg))",
                                  }}
                                />
                              </div>
                              <span className="font-mono text-xs text-[var(--muted)] w-8 text-right">
                                {r.count}
                              </span>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* Footer */}
              <div className="px-6 md:px-8 py-4 border-t border-[var(--border)] flex items-center justify-between">
                <span className="text-xs text-[var(--muted-light)]">
                  Short URL: <span className="font-mono text-[var(--accent-light)]">{analytics.shortCode}</span>
                </span>
                <button
                  onClick={() => setAnalytics(null)}
                  className="text-xs text-[var(--muted)] hover:text-[var(--fg)] transition-colors"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Analytics loading */}
      {analyticsLoading && !analytics && (
        <div className="fixed inset-0 z-50 flex items-start justify-center pt-16 overflow-auto">
          <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" />
          <div className="relative w-full max-w-3xl mx-4">
            <div className="bg-[var(--bg)] border border-[var(--border)] rounded-3xl shadow-2xl p-8">
              <div className="flex items-center justify-center py-16">
                <svg className="animate-spin w-6 h-6 text-[var(--accent-light)]" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Toast */}
      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}
