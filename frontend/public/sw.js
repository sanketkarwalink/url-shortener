const CACHE = "url-shortener-v1";

self.addEventListener("install", (e) => {
  self.skipWaiting();
});

self.addEventListener("activate", (e) => {
  e.waitUntil(clients.claim());
});

self.addEventListener("fetch", (e) => {
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
