const CACHE = "url-shortener-v1";
const API_PATTERN = /^\/api\//;

self.addEventListener("install", (e) => {
  self.skipWaiting();
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    Promise.all([
      clients.claim(),
      caches.keys().then((keys) => Promise.all(keys.map((k) => caches.delete(k)))),
    ])
  );
});

self.addEventListener("fetch", (e) => {
  const url = new URL(e.request.url);
  if (API_PATTERN.test(url.pathname)) {
    return;
  }
  e.respondWith(
    (async () => {
      const r = await caches.match(e.request);
      if (r) return r;
      const res = await fetch(e.request);
      if (res.ok && e.request.method === "GET") {
        const c = await caches.open(CACHE);
        c.put(e.request, res.clone());
      }
      return res;
    })()
  );
});
