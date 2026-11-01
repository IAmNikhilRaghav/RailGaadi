/**
 * RailGaadi Service Worker — Phase 2 PWA
 * Caches app shell, static assets, and provides offline fallback
 */

const CACHE_NAME      = "railgaadi-v3";
const TILES_CACHE_NAME= "railgaadi-tiles-v1";
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
  "/css/routesearch.css",
  "/css/mobile.css",
  "/js/app.js",
  "/manifest.json",
  "/data/stations.json",
  "/data/trains_index.json"
];

/* ── Install: pre-cache static shell ────────────────────── */
self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
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
          .filter(key => key !== CACHE_NAME && key !== TILES_CACHE_NAME)
          .map(key => caches.delete(key))
      )
    )
  );
  self.clients.claim();
});

/* ── Fetch: Cache-first for map assets/tiles/fonts, Network-first with fallback for APIs ── */
self.addEventListener("fetch", event => {
  const url = new URL(event.request.url);

  // Skip non-GET
  if (event.request.method !== "GET") return;

  // 1. Static Map Libraries, Fonts & Raster Tiles — Cache First (huge bandwidth saver on 2G/3G)
  const isMapAssetOrTile =
    url.hostname.includes("cartocdn.com") ||
    url.hostname.includes("maptiler.com") ||
    url.hostname.includes("unpkg.com") ||
    url.hostname.includes("cdnjs.cloudflare.com") ||
    url.hostname.includes("fonts.googleapis.com") ||
    url.hostname.includes("fonts.gstatic.com");

  if (isMapAssetOrTile) {
    event.respondWith(
      caches.open(TILES_CACHE_NAME).then(cache => {
        return cache.match(event.request).then(cached => {
          if (cached) return cached;
          return fetch(event.request).then(res => {
            if (res && res.status === 200) {
              cache.put(event.request, res.clone());
            }
            return res;
          }).catch(() => cached);
        });
      })
    );
    return;
  }

  // 2. Dynamic Live APIs (RailRadar, Weather, Elevation) — Network First with Stale-While-Revalidate
  const isDynamicApi =
    url.pathname.startsWith("/api/railradar") ||
    url.hostname.includes("railradar.in") ||
    url.hostname.includes("openweathermap.org") ||
    url.hostname.includes("opentopography.org") ||
    url.hostname.includes("overpass-api.de");

  if (isDynamicApi) {
    // Let browser network handle live API requests (with Vercel Edge caching headers)
    return;
  }

  // 3. App Shell & Local Assets
  event.respondWith(
    caches.match(event.request).then(cached => {
      if (cached) {
        // Return cached immediately, then revalidate in background
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
