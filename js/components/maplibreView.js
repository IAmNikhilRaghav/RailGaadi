/**
 * RailGaadi Interactive Journey Map — v2
 * MapLibre GL Integration; fully driven from store.liveRoute & trackerService
 * No hardcoded train data — gracefully handles loading / missing geometry
 */

import { store } from "../state/store.js";
import { trackerService } from "../services/trackerService.js";
import { GeoService } from "../services/geoService.js";
import { ENV } from "../config/env.js";

let mapInstance       = null;
let trainMarkerEl     = null;
let trainMapMarker    = null;
let stationMarkers    = [];
let lastTrainId       = null;
let isUserInteracting = false;
let loadedMapStyleKey = null;

/* ── Map Styles ───────────────────────────────────────────── */

const MAPTILER_STYLES = {
  light:     `https://api.maptiler.com/maps/streets-v2/style.json?key=${ENV.MAPTILER_API_KEY}`,
  dark:      `https://api.maptiler.com/maps/dataviz-dark/style.json?key=${ENV.MAPTILER_API_KEY}`,
  satellite: `https://api.maptiler.com/maps/hybrid/style.json?key=${ENV.MAPTILER_API_KEY}`,
  terrain:   `https://api.maptiler.com/maps/topo-v2/style.json?key=${ENV.MAPTILER_API_KEY}`
};

const CARTO_STYLES = {
  light: {
    version: 8, glyphs: "https://fonts.openmaptiles.org/{fontstack}/{range}.pbf",
    sources: { carto: { type: "raster", tiles: ["https://a.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png"], tileSize: 256 } },
    layers:  [{ id: "carto", type: "raster", source: "carto" }]
  },
  dark: {
    version: 8, glyphs: "https://fonts.openmaptiles.org/{fontstack}/{range}.pbf",
    sources: { carto: { type: "raster", tiles: ["https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png"], tileSize: 256 } },
    layers:  [{ id: "carto", type: "raster", source: "carto" }]
  }
};

function getStyle(mapStyle) {
  if (ENV.MAPTILER_API_KEY) return MAPTILER_STYLES[mapStyle] || MAPTILER_STYLES.light;
  return mapStyle === "dark" ? CARTO_STYLES.dark : CARTO_STYLES.light;
}

/* ── Render ───────────────────────────────────────────────── */

export function renderMapSection() {
  const state = store.getState();
  const { activeTrainId, activeTrain, liveStatus, isLoadingRoute } = state;

  const trainName = activeTrain?.name || (activeTrainId ? `Train #${activeTrainId}` : "—");
  const speed     = liveStatus?.speedKmh ? `${liveStatus.speedKmh} km/h` : "—";

  return `
    <div class="map-wrapper" aria-label="Interactive Train Route Map">
      <!-- Map canvas -->
      <div id="railMap" class="map-container"></div>

      <!-- Empty / Loading overlay -->
      ${!activeTrainId ? `
        <div class="map-empty-overlay" id="mapEmptyOverlay">
          <div style="text-align:center;color:var(--color-text-muted);">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom:12px;opacity:.5;">
              <circle cx="12" cy="12" r="10"/><path d="M12 8v4l2 2"/>
            </svg>
            <p style="font-weight:600;font-size:14px;">Search a train to display its route</p>
          </div>
        </div>
      ` : isLoadingRoute ? `
        <div class="map-empty-overlay" id="mapEmptyOverlay">
          <div style="text-align:center;color:var(--color-text-muted);">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--color-accent)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="animation:spin 0.8s linear infinite;margin-bottom:10px;">
              <path d="M21 12a9 9 0 1 1-6.22-8.56"/>
            </svg>
            <p style="font-size:13px;">Loading route…</p>
          </div>
        </div>
      ` : ""}

      <!-- Floating Status Badge -->
      <div class="map-floating-status" id="mapStatusBadge">
        <span class="pulse-dot"></span>
        <span style="font-size:12px;font-weight:700;color:var(--color-text-primary);">${trainName}</span>
        <span class="floating-speed-label" id="mapSpeedLabel">${speed}</span>
      </div>

      <!-- Floating Controls -->
      <div class="map-controls-floating">
        <button class="map-control-btn ${state.followTrain ? "active" : ""}" id="mapFollowToggleBtn" title="Follow Train Camera" aria-label="Toggle camera follow mode">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="3"/>
            <line x1="12" y1="2" x2="12" y2="5"/><line x1="12" y1="19" x2="12" y2="22"/>
            <line x1="2" y1="12" x2="5" y2="12"/><line x1="19" y1="12" x2="22" y2="12"/>
          </svg>
        </button>
        <button class="map-control-btn" id="mapZoomInBtn"  title="Zoom In"  aria-label="Zoom in">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
        </button>
        <button class="map-control-btn" id="mapZoomOutBtn" title="Zoom Out" aria-label="Zoom out">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
        </button>
        <button class="map-control-btn" id="mapFullscreenBtn" title="Toggle Fullscreen" aria-label="Toggle fullscreen">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/>
          </svg>
        </button>
      </div>

      <!-- Recenter Button -->
      <button class="map-recenter-btn" id="mapRecenterBtn" aria-label="Recenter on train">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="3 11 22 2 13 21 11 13 3 11"/>
        </svg>
        <span>Re-center on Train</span>
      </button>

      <style>@keyframes spin { to { transform: rotate(360deg); } }</style>
    </div>
  `;
}

/* ── Init / Update ────────────────────────────────────────── */

export function initOrUpdateMap() {
  const container = document.getElementById("railMap");
  if (!container) return;
  if (typeof window.maplibregl === "undefined") { console.warn("MapLibre GL not available"); return; }

  // Check if existing map instance has been detached from DOM
  if (mapInstance) {
    try {
      const curCont = mapInstance.getContainer();
      if (!curCont || curCont !== container || !document.body.contains(curCont)) {
        mapInstance.remove();
        mapInstance = null;
        trainMarkerEl = null;
        trainMapMarker = null;
        stationMarkers = [];
      }
    } catch {
      mapInstance = null;
      trainMarkerEl = null;
      trainMapMarker = null;
      stationMarkers = [];
    }
  }

  const state   = store.getState();
  const route   = state.liveRoute;
  const mapStyle = getStyle(state.mapStyle);

  // Determine center: from live GPS > route start > India centre
  const pos        = trackerService.getCurrentPosition();
  const defaultPos = route?.routeCoordinates?.[0];
  const center     = pos ? [pos.lon, pos.lat]
                   : defaultPos ? defaultPos
                   : [78.96, 22.59]; // India centre

  const trainChanged = state.activeTrainId !== lastTrainId;

  if (!mapInstance) {
    try {
      mapInstance = new window.maplibregl.Map({
        container: "railMap",
        style: mapStyle,
        center,
        zoom: 5.8,
        pitch: 20,
        attributionControl: false
      });
      mapInstance.on("dragstart", () => { isUserInteracting = true; store.setFollowTrain(false); showRecenterBtn(true); });
      mapInstance.on("load", () => { if (route) drawFullRoute(route, pos); });
      loadedMapStyleKey = state.mapStyle;
    } catch (e) {
      console.error("Map init error:", e);
    }
    lastTrainId = state.activeTrainId;
    return;
  }

  let styleChanged = loadedMapStyleKey !== state.mapStyle;
  if (styleChanged) {
    loadedMapStyleKey = state.mapStyle;
    mapInstance.setStyle(mapStyle);
    mapInstance.once("styledata", () => {
      if (route) drawFullRoute(route, pos);
    });
    lastTrainId = state.activeTrainId;
    return;
  }

  lastTrainId = state.activeTrainId;

  if (!mapInstance.isStyleLoaded()) {
    mapInstance.once("load", () => { if (route) drawFullRoute(route, pos); });
    return;
  }

  if (route) {
    if (trainChanged || !mapInstance.getSource("route-completed")) {
      drawFullRoute(route, pos);
    } else {
      updateRouteLayers(route, pos);
      updateTrainMarker(pos);
    }
  }

  // Camera follow
  if (state.followTrain && pos && mapInstance) {
    mapInstance.easeTo({ center: [pos.lon, pos.lat], duration: 1000 });
    showRecenterBtn(false);
  }

  // Update speed label
  const speedEl = document.getElementById("mapSpeedLabel");
  if (speedEl && state.liveStatus?.speedKmh) speedEl.textContent = `${state.liveStatus.speedKmh} km/h`;
}

/* ── Route Drawing ────────────────────────────────────────── */

function removeLayerSafe(id)  { if (mapInstance.getLayer(id))  mapInstance.removeLayer(id); }
function removeSourceSafe(id) { if (mapInstance.getSource(id)) mapInstance.removeSource(id); }

function drawFullRoute(route, pos) {
  if (!mapInstance || !route?.routeCoordinates?.length) return;

  // Teardown existing layers
  ["route-completed-glow","route-completed-line","route-remaining-line"].forEach(removeLayerSafe);
  ["route-completed","route-remaining"].forEach(removeSourceSafe);
  stationMarkers.forEach(m => m.remove());
  stationMarkers = [];
  if (trainMapMarker) { trainMapMarker.remove(); trainMapMarker = null; trainMarkerEl = null; }

  const { completed, remaining } = pos
    ? GeoService.splitRoute(route.routeCoordinates, pos)
    : { completed: [], remaining: route.routeCoordinates };

  // Completed segment
  mapInstance.addSource("route-completed", { type: "geojson", data: lineFeature(completed) });
  mapInstance.addLayer({ id: "route-completed-glow", type: "line", source: "route-completed",
    layout: { "line-join": "round", "line-cap": "round" },
    paint: { "line-color": "#3b82f6", "line-width": 10, "line-opacity": 0.3, "line-blur": 4 }
  });
  mapInstance.addLayer({ id: "route-completed-line", type: "line", source: "route-completed",
    layout: { "line-join": "round", "line-cap": "round" },
    paint: { "line-color": "#1e40af", "line-width": 4.5 }
  });

  // Remaining segment
  mapInstance.addSource("route-remaining", { type: "geojson", data: lineFeature(remaining) });
  mapInstance.addLayer({ id: "route-remaining-line", type: "line", source: "route-remaining",
    layout: { "line-join": "round", "line-cap": "round" },
    paint: { "line-color": "#94a3b8", "line-width": 3, "line-dasharray": [2, 2] }
  });

  // Station markers
  const curIdx = trackerService.getCurrentStationIndex();
  (route.stations || []).forEach((stn, idx) => {
    if (!stn.latitude || !stn.longitude) return;
    if (!GeoService.isValidIndiaCord(stn.longitude, stn.latitude)) return;

    const el = document.createElement("div");
    el.className = "station-map-marker";
    if (idx < curIdx)  el.classList.add("passed");
    if (idx === curIdx) el.classList.add("current-hub");

    const popup = new window.maplibregl.Popup({ offset: 12 }).setHTML(`
      <div style="font-family:sans-serif;min-width:160px;">
        <strong style="font-size:14px;color:#0f172a;">${stn.name}${stn.code ? ` (${stn.code})` : ""}</strong>
        <div style="font-size:12px;color:#64748b;margin-top:4px;">
          ${stn.scheduledArr !== "—" ? `Arr: <strong>${stn.scheduledArr}</strong>` : ""}
          ${stn.scheduledDep !== "—" ? ` · Dep: <strong>${stn.scheduledDep}</strong>` : ""}
        </div>
        ${stn.platform ? `<div style="font-size:11px;color:#1e40af;margin-top:2px;">Platform ${stn.platform}</div>` : ""}
        <div style="font-size:11px;color:#64748b;">${stn.distanceKm ? `${stn.distanceKm} km from origin` : ""}</div>
      </div>
    `);

    const marker = new window.maplibregl.Marker({ element: el })
      .setLngLat([stn.longitude, stn.latitude])
      .setPopup(popup)
      .addTo(mapInstance);
    stationMarkers.push(marker);
  });

  // Train marker
  if (pos) {
    trainMarkerEl = document.createElement("div");
    trainMarkerEl.className = "train-custom-marker";
    trainMarkerEl.innerHTML = `
      <div class="train-marker-pulse"></div>
      <div class="train-marker-inner" style="transform:rotate(${pos.bearing || 0}deg);">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <rect width="16" height="16" x="4" y="3" rx="2"/>
          <path d="M4 11h16"/><path d="M12 3v8"/>
          <path d="m8 19-2 3"/><path d="m18 22-2-3"/>
        </svg>
      </div>
    `;
    trainMapMarker = new window.maplibregl.Marker({ element: trainMarkerEl })
      .setLngLat([pos.lon, pos.lat])
      .addTo(mapInstance);
  }

  // Fit map to route bounds
  const coords = route.routeCoordinates.filter(c => GeoService.isValidIndiaCord(c[0], c[1]));
  if (coords.length > 1) {
    const bounds = coords.reduce(
      (b, c) => b.extend(c),
      new window.maplibregl.LngLatBounds(coords[0], coords[0])
    );
    mapInstance.fitBounds(bounds, { padding: 60, duration: 1200, maxZoom: 8 });
  }
}

function updateRouteLayers(route, pos) {
  if (!mapInstance || !route?.routeCoordinates?.length) return;
  const { completed, remaining } = pos
    ? GeoService.splitRoute(route.routeCoordinates, pos)
    : { completed: [], remaining: route.routeCoordinates };

  const cSrc = mapInstance.getSource("route-completed");
  const rSrc = mapInstance.getSource("route-remaining");
  if (cSrc) cSrc.setData(lineFeature(completed));
  if (rSrc) rSrc.setData(lineFeature(remaining));
}

function updateTrainMarker(pos) {
  if (!pos || !trainMapMarker) return;
  trainMapMarker.setLngLat([pos.lon, pos.lat]);
  const inner = trainMarkerEl?.querySelector(".train-marker-inner");
  if (inner) inner.style.transform = `rotate(${pos.bearing || 0}deg)`;
}

function lineFeature(coordinates) {
  return { type: "Feature", properties: {}, geometry: { type: "LineString", coordinates } };
}

/* ── Events ───────────────────────────────────────────────── */

export function attachMapEvents() {
  const followBtn = document.getElementById("mapFollowToggleBtn");
  if (followBtn) {
    followBtn.addEventListener("click", () => {
      const follow = !store.getState().followTrain;
      store.setFollowTrain(follow);
      followBtn.classList.toggle("active", follow);
      if (follow && mapInstance) {
        const pos = trackerService.getCurrentPosition();
        if (pos) { mapInstance.easeTo({ center: [pos.lon, pos.lat], zoom: 7 }); showRecenterBtn(false); }
      }
    });
  }

  const recenterBtn = document.getElementById("mapRecenterBtn");
  if (recenterBtn) {
    recenterBtn.addEventListener("click", () => {
      store.setFollowTrain(true);
      const pos = trackerService.getCurrentPosition();
      if (mapInstance && pos) mapInstance.flyTo({ center: [pos.lon, pos.lat], zoom: 7 });
      showRecenterBtn(false);
    });
  }

  const zoomIn  = document.getElementById("mapZoomInBtn");
  const zoomOut = document.getElementById("mapZoomOutBtn");
  if (zoomIn)  zoomIn.addEventListener("click",  () => mapInstance?.zoomIn());
  if (zoomOut) zoomOut.addEventListener("click", () => mapInstance?.zoomOut());

  const fsBtn = document.getElementById("mapFullscreenBtn");
  if (fsBtn) {
    fsBtn.addEventListener("click", () => {
      const wrapper = document.querySelector(".map-wrapper");
      if (!document.fullscreenElement) wrapper?.requestFullscreen().catch(() => {});
      else document.exitFullscreen().catch(() => {});
    });
  }
}

function showRecenterBtn(show) {
  document.getElementById("mapRecenterBtn")?.classList.toggle("visible", show);
}

export function focusLocationOnMap(lon, lat, name) {
  if (!mapInstance) return;
  mapInstance.flyTo({ center: [lon, lat], zoom: 10, duration: 1200 });
  new window.maplibregl.Popup({ offset: 12 })
    .setLngLat([lon, lat])
    .setHTML(`<strong>${name}</strong>`)
    .addTo(mapInstance);
}

window.addEventListener("resize", () => {
  if (mapInstance) {
    try { mapInstance.resize(); } catch (e) {}
  }
});
