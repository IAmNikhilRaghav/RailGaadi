/**
 * RailGaadi Service Worker — Phase 2 PWA
 * Caches app shell, static assets, and provides offline fallback
 */

const CACHE_NAME      = "railgaadi-v2";
const STATIC_ASSETS   = [
  "/",
  "/index.html",
  "/css/tokens.css",
  "/css/base.css",
  "/css/components.css",
  "/css/map.css",
  "/css/analytics.css",
  "/css/companion.css",
  "/css/phase2.css",
  "/js/app.js",
  "/manifest.json"
];

/* ── Install: pre-cache static shell ────────────────────── */
self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      // Cache what we can; don't fail installation if some fail
      return Promise.allSettled(
        STATIC_ASSETS.map(url =>
          cache.add(url).catch(err => {
            console.warn(`[SW] Failed to cache ${url}:`, err.message);
          })
        )
      );
    })
  );
  self.skipWaiting();
});

/* ── Activate: clean up old caches ──────────────────────── */
self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(
        keys
          .filter(key => key !== CACHE_NAME)
          .map(key => caches.delete(key))
      )
    )
  );
  self.clients.claim();
});

/* ── Fetch: network-first for API calls, cache-first for assets ── */
self.addEventListener("fetch", event => {
  const url = new URL(event.request.url);

  // Skip non-GET and cross-origin requests to live APIs
  if (event.request.method !== "GET") return;
  if (
    url.pathname.startsWith("/api/railradar") ||
    url.hostname.includes("openweathermap.org") ||
    url.hostname.includes("opentopography.org") ||
    url.hostname.includes("overpass-api.de") ||
    url.hostname.includes("maptiler.com") ||
    url.hostname.includes("maplibre") ||
    url.hostname.includes("fonts.googleapis.com") ||
    url.hostname.includes("unpkg.com")
  ) {
    // Network-first, no caching for live data / CDN resources
    return;
  }

  event.respondWith(
    caches.match(event.request).then(cached => {
      if (cached) {
        // Return cached, then update in background
        fetch(event.request)
          .then(fresh => {
            if (fresh && fresh.ok) {
              caches.open(CACHE_NAME).then(cache => cache.put(event.request, fresh));
            }
          })
          .catch(() => {});
        return cached;
      }

      // Not in cache — fetch from network
      return fetch(event.request).then(response => {
        if (response && response.ok && response.type !== "opaque") {
          const clone = response.clone();
          caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone));
        }
        return response;
      }).catch(() => {
        // Offline fallback — return index.html for navigation requests
        if (event.request.mode === "navigate") {
          return caches.match("/index.html");
        }
      });
    })
  );
});

/* ── Background Sync (future: queue status fetches offline) ── */
self.addEventListener("sync", event => {
  if (event.tag === "sync-train-status") {
    // Placeholder — future implementation
    console.log("[SW] Background sync triggered for train status");
  }
});
