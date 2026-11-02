/**
 * RailGaadi Interactive Journey Map — v3
 * MapLibre GL Integration; fully driven from store.liveRoute & trackerService
 * High-performance, low-bandwidth optimized, resilient first-time loading & device GPS location.
 */

import { store } from "../state/store.js";
import { trackerService } from "../services/trackerService.js";
import { GeoService } from "../services/geoService.js";
import { ENV } from "../config/env.js";

let mapInstance        = null;
let trainMarkerEl      = null;
let trainMapMarker     = null;
let userLocationMarker = null;
let userLocationEl     = null;
let stationMarkers     = [];
let lastTrainId        = null;
let isUserInteracting  = false;
let loadedMapStyleKey  = null;
let mapResizeObserver   = null;
let isMapLibreLoading  = false;

/* ── Map Styles ───────────────────────────────────────────── */

const MAPTILER_STYLES = {
  light:     `https://api.maptiler.com/maps/streets-v2/style.json?key=${ENV.MAPTILER_API_KEY}`,
  dark:      `https://api.maptiler.com/maps/dataviz-dark/style.json?key=${ENV.MAPTILER_API_KEY}`,
  satellite: `https://api.maptiler.com/maps/hybrid/style.json?key=${ENV.MAPTILER_API_KEY}`,
  terrain:   `https://api.maptiler.com/maps/topo-v2/style.json?key=${ENV.MAPTILER_API_KEY}`
};

const CARTO_STYLES = {
  light: {
    version: 8,
    glyphs: "https://fonts.openmaptiles.org/{fontstack}/{range}.pbf",
    sources: {
      carto: {
        type: "raster",
        tiles: [
          "https://a.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png",
          "https://b.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png",
          "https://c.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png",
          "https://d.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png"
        ],
        tileSize: 256
      }
    },
    layers: [{ id: "carto", type: "raster", source: "carto" }]
  },
  dark: {
    version: 8,
    glyphs: "https://fonts.openmaptiles.org/{fontstack}/{range}.pbf",
    sources: {
      carto: {
        type: "raster",
        tiles: [
          "https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png",
          "https://b.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png",
          "https://c.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png",
          "https://d.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png"
        ],
        tileSize: 256
      }
    },
    layers: [{ id: "carto", type: "raster", source: "carto" }]
  }
};

function getStyle(mapStyle) {
  if (ENV.MAPTILER_API_KEY) return MAPTILER_STYLES[mapStyle] || MAPTILER_STYLES.light;
  return mapStyle === "dark" ? CARTO_STYLES.dark : CARTO_STYLES.light;
}

/** Ensure MapLibre is ready even on slow networks without requiring page refresh */
function ensureMapLibreLoaded(callback) {
  if (typeof window.maplibregl !== "undefined") {
    callback();
    return;
  }
  if (isMapLibreLoading) {
    const timer = setInterval(() => {
      if (typeof window.maplibregl !== "undefined") {
        clearInterval(timer);
        callback();
      }
    }, 80);
    setTimeout(() => clearInterval(timer), 12000);
    return;
  }

  isMapLibreLoading = true;
  let attempts = 0;
  const poll = setInterval(() => {
    attempts++;
    if (typeof window.maplibregl !== "undefined") {
      clearInterval(poll);
      isMapLibreLoading = false;
      callback();
    } else if (attempts > 25) {
      clearInterval(poll);
      // Fallback: inject backup CDN
      const script = document.createElement("script");
      script.src = "https://cdnjs.cloudflare.com/ajax/libs/maplibre-gl/5.1.0/maplibre-gl.js";
      script.crossOrigin = "anonymous";
      script.onload = () => { isMapLibreLoading = false; callback(); };
      script.onerror = () => { isMapLibreLoading = false; console.error("MapLibre CDN unavailable"); };
      document.head.appendChild(script);
    }
  }, 100);
}

/* ── Render ───────────────────────────────────────────────── */

export function renderMapSection() {
  const state = store.getState();
  const { activeTrainId, activeTrain, liveStatus, isLoadingRoute, liveRoute } = state;

  const trainName = activeTrain?.name || (activeTrainId ? `Train #${activeTrainId}` : "—");
  const speed     = liveStatus?.speedKmh ? `${liveStatus.speedKmh} km/h` : "—";
  const hasRoute  = !!(liveRoute?.routeCoordinates?.length);

  return `
    <div class="map-wrapper" aria-label="Interactive Train Route Map">
      <!-- Map canvas -->
      <div id="railMap" class="map-container"></div>

      <!-- Empty / Loading overlay -->
      <div class="map-empty-overlay" id="mapEmptyOverlay" style="${hasRoute ? "display:none;opacity:0;" : "display:flex;opacity:1;"}">
        ${!activeTrainId ? `
          <div style="text-align:center;color:var(--color-text-muted);">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom:12px;opacity:.5;">
              <circle cx="12" cy="12" r="10"/><path d="M12 8v4l2 2"/>
            </svg>
            <p style="font-weight:600;font-size:14px;">Search a train to display its route</p>
          </div>
        ` : isLoadingRoute && !hasRoute ? `
          <div style="text-align:center;color:var(--color-text-muted);">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--color-accent)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="animation:spin 0.8s linear infinite;margin-bottom:10px;">
              <path d="M21 12a9 9 0 1 1-6.22-8.56"/>
            </svg>
            <p style="font-size:13px;font-weight:600;">Loading live route &amp; telemetry…</p>
          </div>
        ` : ""}
      </div>

      <!-- Floating Status Badge -->
      <div class="map-floating-status" id="mapStatusBadge">
        <span class="pulse-dot"></span>
        <span style="font-size:12px;font-weight:700;color:var(--color-text-primary);">${trainName}</span>
        <span class="floating-speed-label" id="mapSpeedLabel">${speed}</span>
      </div>

      <!-- Floating Controls -->
      <div class="map-controls-floating" id="mapControlsFloating">
        <!-- Three-dots toggle button for small screen view -->
        <button class="map-control-btn map-more-tools-btn" id="mapMoreToolsBtn" title="More map tools (⋮)" aria-label="Toggle map tools">
          <svg class="more-tools-icon-dots" width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
            <circle cx="12" cy="5" r="2.2"/>
            <circle cx="12" cy="12" r="2.2"/>
            <circle cx="12" cy="19" r="2.2"/>
          </svg>
          <svg class="more-tools-icon-close" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="display:none;">
            <path d="M18 6 6 18M6 6l12 12"/>
          </svg>
        </button>

        <!-- Google Maps-style Dynamic Compass (rotates with heading, tap resets North) -->
        <button class="map-control-btn map-compass-btn map-collapsible-tool" id="mapCompassBtn" title="Reset North (🧭)" aria-label="Reset orientation to North">
          <svg class="compass-needle" id="compassNeedleSvg" width="22" height="22" viewBox="0 0 24 24" style="transition:transform 0.15s ease-out;display:block;">
            <polygon points="12 2, 7.5 12, 12 9.5" fill="#ef4444" stroke="#dc2626" stroke-width="0.5"/>
            <polygon points="12 2, 16.5 12, 12 9.5" fill="#f87171" stroke="#dc2626" stroke-width="0.5"/>
            <polygon points="12 22, 7.5 12, 12 14.5" fill="#64748b" stroke="#475569" stroke-width="0.5"/>
            <polygon points="12 22, 16.5 12, 12 14.5" fill="#94a3b8" stroke="#475569" stroke-width="0.5"/>
            <circle cx="12" cy="12" r="2.2" fill="#ffffff" stroke="#0f172a" stroke-width="1.2"/>
          </svg>
        </button>

        <!-- 3D Perspective Viewpoint Toggle -->
        <button class="map-control-btn map-collapsible-tool" id="mapPitchBtn" title="Toggle 3D Viewpoint" aria-label="Toggle 3D perspective viewpoint">
          <span class="pitch-mode-label" style="font-size:11px;font-weight:800;letter-spacing:-0.5px;">3D</span>
        </button>

        <!-- Rotate Anti-Clockwise -->
        <button class="map-control-btn map-collapsible-tool" id="mapRotateLeftBtn" title="Rotate Anti-Clockwise (⟲)" aria-label="Rotate Anti-Clockwise">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
            <path d="M3 3v5h5"/>
          </svg>
        </button>

        <!-- Rotate Clockwise -->
        <button class="map-control-btn map-collapsible-tool" id="mapRotateRightBtn" title="Rotate Clockwise (⟳)" aria-label="Rotate Clockwise">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12a9 9 0 1 1-9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/>
            <path d="M21 3v5h-5"/>
          </svg>
        </button>

        <div class="map-controls-divider map-collapsible-tool"></div>

        <!-- User Location / GPS Button -->
        <button class="map-control-btn map-collapsible-tool" id="mapLocateUserBtn" title="My Current Location" aria-label="Find my location on map">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="4"/><path d="M12 2v3"/><path d="M12 19v3"/><path d="M2 12h3"/><path d="M19 12h3"/>
          </svg>
        </button>

        <!-- Follow Train Camera Toggle -->
        <button class="map-control-btn map-collapsible-tool ${state.followTrain ? "active" : ""}" id="mapFollowToggleBtn" title="Follow Train Camera" aria-label="Toggle camera follow mode">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="3"/>
            <line x1="12" y1="2" x2="12" y2="5"/><line x1="12" y1="19" x2="12" y2="22"/>
            <line x1="2" y1="12" x2="5" y2="12"/><line x1="19" y1="12" x2="22" y2="12"/>
          </svg>
        </button>

        <button class="map-control-btn map-collapsible-tool" id="mapZoomInBtn"  title="Zoom In"  aria-label="Zoom in">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
        </button>

        <button class="map-control-btn map-collapsible-tool" id="mapZoomOutBtn" title="Zoom Out" aria-label="Zoom out">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
        </button>

        <!-- Fullscreen Button — always accessible -->
        <button class="map-control-btn" id="mapFullscreenBtn" title="Toggle Fullscreen" aria-label="Toggle fullscreen">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/>
          </svg>
        </button>
      </div>

      <!-- Fullscreen Top-Right Tools Initializer -->
      <button class="map-edge-peek-tab" id="mapEdgePeekTab" title="Show map tools" aria-label="Show map tools">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 18-6-6 6-6"/>
        </svg>
      </button>

      <!-- Recenter Button (compact modern pill) -->
      <button class="map-recenter-btn" id="mapRecenterBtn" aria-label="Recenter on train">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="3 11 22 2 13 21 11 13 3 11"/>
        </svg>
        <span>Re-center</span>
      </button>

      <style>@keyframes spin { to { transform: rotate(360deg); } }</style>
    </div>
  `;
}

/* ── Init / Update ────────────────────────────────────────── */

export function initOrUpdateMap() {
  const container = document.getElementById("railMap");
  if (!container) return;

  if (typeof window.maplibregl === "undefined") {
    ensureMapLibreLoaded(() => initOrUpdateMap());
    return;
  }

  const state    = store.getState();
  const route    = state.liveRoute;
  const mapStyle = getStyle(state.mapStyle);
  const pos      = trackerService.getCurrentPosition();

  // Dynamic Overlay Management — updates lively without tearing down canvas
  const overlay = document.getElementById("mapEmptyOverlay");
  if (overlay) {
    if (!state.activeTrainId) {
      overlay.style.display = "flex";
      overlay.style.opacity = "1";
      overlay.style.pointerEvents = "auto";
      overlay.innerHTML = `
        <div style="text-align:center;color:var(--color-text-muted);">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom:12px;opacity:.5;">
            <circle cx="12" cy="12" r="10"/><path d="M12 8v4l2 2"/>
          </svg>
          <p style="font-weight:600;font-size:14px;">Search a train to display its route</p>
        </div>
      `;
    } else if (state.isLoadingRoute && !route?.routeCoordinates?.length) {
      overlay.style.display = "flex";
      overlay.style.opacity = "1";
      overlay.style.pointerEvents = "auto";
      overlay.innerHTML = `
        <div style="text-align:center;color:var(--color-text-muted);">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--color-accent)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="animation:spin 0.8s linear infinite;margin-bottom:10px;">
            <path d="M21 12a9 9 0 1 1-6.22-8.56"/>
          </svg>
          <p style="font-size:13px;font-weight:600;">Loading live route &amp; telemetry…</p>
        </div>
      `;
    } else if (route?.routeCoordinates?.length) {
      overlay.style.transition = "opacity 0.25s ease";
      overlay.style.opacity = "0";
      overlay.style.pointerEvents = "none";
      setTimeout(() => {
        if (overlay && state.liveRoute?.routeCoordinates?.length) overlay.style.display = "none";
      }, 250);
    }
  }

  // Check if existing map instance has been detached from DOM
  if (mapInstance) {
    try {
      const curCont = mapInstance.getContainer();
      if (!curCont || curCont !== container || !document.body.contains(curCont)) {
        mapInstance.remove();
        mapInstance        = null;
        trainMarkerEl      = null;
        trainMapMarker     = null;
        userLocationMarker = null;
        stationMarkers     = [];
      }
    } catch {
      mapInstance        = null;
      trainMarkerEl      = null;
      trainMapMarker     = null;
      userLocationMarker = null;
      stationMarkers     = [];
    }
  }

  // Determine center: from live GPS > route start > India centre
  const defaultPos = route?.routeCoordinates?.[0];
  const center     = pos && !isNaN(pos.lon) && !isNaN(pos.lat)
                   ? [pos.lon, pos.lat]
                   : defaultPos && !isNaN(defaultPos[0]) && !isNaN(defaultPos[1])
                   ? defaultPos
                   : [78.96, 22.59]; // India centre

  const trainChanged = state.activeTrainId !== lastTrainId;

  if (!mapInstance) {
    try {
      mapInstance = new window.maplibregl.Map({
        container: "railMap",
        style: mapStyle,
        center,
        zoom: 5.6,
        pitch: 0,
        bearing: 0,
        maxPitch: 65,
        attributionControl: false,
        pixelRatio: Math.min(window.devicePixelRatio || 1, 2),
        fadeDuration: 0,
        crossSourceCollisions: false,
        dragPan: {
          linearity: 0.25,
          maxSpeed: 1800,
          deceleration: 2400
        },
        doubleClickZoom: true,
        touchZoomRotate: true,
        dragRotate: true,
        touchPitch: true,
        pitchWithRotate: true,
        cooperativeGestures: false,
        renderWorldCopies: true,
        maxTileCacheSize: 150
      });

      // Enable dynamic 360° rotation (clockwise & anti-clockwise) and 3D perspective viewpoint
      if (mapInstance.touchZoomRotate) {
        mapInstance.touchZoomRotate.enable();
        mapInstance.touchZoomRotate.enableRotation();
      }
      if (mapInstance.touchPitch) {
        mapInstance.touchPitch.enable();
      }
      if (mapInstance.dragRotate) {
        mapInstance.dragRotate.enable();
      }

      // Track all user gestures: pinch-zoom, pan, rotation, pitch tilt, touch, wheel, click-drag
      // Immediately disconnect followTrain so telemetry ticks do NOT interrupt user gestures!
      const onUserGestureStart = (e) => {
        if (!e || e.originalEvent) {
          isUserInteracting = true;
          store.setFollowTrain(false);
          showRecenterBtn(true);
        }
      };

      mapInstance.on("movestart", onUserGestureStart);
      mapInstance.on("zoomstart", onUserGestureStart);
      mapInstance.on("dragstart", onUserGestureStart);
      mapInstance.on("rotatestart", onUserGestureStart);
      mapInstance.on("pitchstart", onUserGestureStart);
      mapInstance.on("touchstart", onUserGestureStart);
      mapInstance.on("wheel", onUserGestureStart);

      mapInstance.on("moveend", () => {
        isUserInteracting = false;
      });

      // Google Maps-style dynamic compass & 3D viewpoint synchronization
      const updateOrientationUI = () => {
        if (!mapInstance) return;
        const bearing = mapInstance.getBearing();
        const pitch   = mapInstance.getPitch();

        const compassNeedle = document.getElementById("compassNeedleSvg");
        if (compassNeedle) {
          compassNeedle.style.transform = `rotate(${-bearing}deg)`;
        }

        const compassBtn = document.getElementById("mapCompassBtn");
        if (compassBtn) {
          const isOffNorth = Math.abs(bearing) > 1 || Math.abs(pitch) > 1;
          compassBtn.classList.toggle("is-active", isOffNorth);
        }

        const pitchBtn = document.getElementById("mapPitchBtn");
        if (pitchBtn) {
          const is3D = pitch > 15;
          pitchBtn.classList.toggle("is-active", is3D);
          const label = pitchBtn.querySelector(".pitch-mode-label");
          if (label) label.textContent = is3D ? "2D" : "3D";
        }
      };

      mapInstance.on("rotate", updateOrientationUI);
      mapInstance.on("pitch",  updateOrientationUI);
      mapInstance.on("move", (e) => {
        updateOrientationUI();
        if (e?.originalEvent) {
          const state = store.getState();
          if (!state.followTrain) {
            showRecenterBtn(true);
          }
        }
      });

      mapInstance.on("load", () => {
        if (route) drawFullRoute(route, pos);
        setTimeout(() => mapInstance?.resize(), 60);
      });
      loadedMapStyleKey = state.mapStyle;
    } catch (e) {
      console.error("[MapLibre] init error:", e);
    }
    lastTrainId = state.activeTrainId;

    // Attach ResizeObserver to keep canvas responsive without requiring window refresh
    if (!mapResizeObserver && container) {
      try {
        mapResizeObserver = new ResizeObserver(() => {
          if (mapInstance) {
            try { mapInstance.resize(); } catch (e) {}
          }
        });
        mapResizeObserver.observe(container);
      } catch (e) {}
    }
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
    mapInstance.once("load", () => {
      if (route) drawFullRoute(route, pos);
      mapInstance?.resize();
    });
    return;
  }

  if (route?.routeCoordinates?.length) {
    if (trainChanged || !mapInstance.getSource("route-completed")) {
      drawFullRoute(route, pos);
    } else {
      updateRouteLayers(route, pos);
      updateTrainMarker(pos);
    }
  }

  // Camera follow — only if user is NOT actively touching/gesturing on map
  if (state.followTrain && !isUserInteracting && pos && mapInstance && !isNaN(pos.lon) && !isNaN(pos.lat)) {
    mapInstance.easeTo({ center: [pos.lon, pos.lat], duration: 1000 });
    showRecenterBtn(false);
  }

  // Force canvas size update only when idle to prevent layout thrashing during touch moves
  if (!isUserInteracting && mapInstance) {
    try { mapInstance.resize(); } catch (e) {}
  }

  // Update speed & badge label
  const speedEl = document.getElementById("mapSpeedLabel");
  if (speedEl && state.liveStatus?.speedKmh) speedEl.textContent = `${state.liveStatus.speedKmh} km/h`;
}

/* ── Route Drawing ────────────────────────────────────────── */

function removeLayerSafe(id)  { if (mapInstance && mapInstance.getLayer(id))  mapInstance.removeLayer(id); }
function removeSourceSafe(id) { if (mapInstance && mapInstance.getSource(id)) mapInstance.removeSource(id); }

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

  try {
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
  } catch (err) {
    console.warn("[MapLibre] layer draw warning:", err);
  }

  // Station markers rendered directly via WebGL GPU layer (0 DOM overhead, smooth 60fps pinch-to-zoom)
  removeLayerSafe("stations-touch-target");
  removeLayerSafe("stations-circle-halo");
  removeLayerSafe("stations-circle");
  removeSourceSafe("stations-data");

  const curIdx = trackerService.getCurrentStationIndex();
  const validStations = (route.stations || []).filter(stn => {
    if (!stn.latitude || !stn.longitude) return false;
    const lat = Number(stn.latitude);
    const lon = Number(stn.longitude);
    return !isNaN(lat) && !isNaN(lon) && GeoService.isValidIndiaCord(lon, lat);
  });

  const stationsGeoJson = {
    type: "FeatureCollection",
    features: validStations.map((stn, idx) => ({
      type: "Feature",
      geometry: { type: "Point", coordinates: [Number(stn.longitude), Number(stn.latitude)] },
      properties: {
        idx,
        name: stn.name,
        code: stn.code,
        scheduledArr: stn.scheduledArr || "—",
        scheduledDep: stn.scheduledDep || "—",
        platform: stn.platform || "",
        distanceKm: stn.distanceKm || 0,
        isPassed: idx < curIdx,
        isCurrent: idx === curIdx,
        isTerminus: idx === 0 || idx === validStations.length - 1
      }
    }))
  };

  try {
    mapInstance.addSource("stations-data", {
      type: "geojson",
      data: stationsGeoJson
    });

    // Outer white halo
    mapInstance.addLayer({
      id: "stations-circle-halo",
      type: "circle",
      source: "stations-data",
      paint: {
        "circle-radius": [
          "case",
          ["boolean", ["get", "isCurrent"], false], 9,
          ["boolean", ["get", "isTerminus"], false], 7.5,
          5.5
        ],
        "circle-color": "#ffffff",
        "circle-stroke-width": 1.5,
        "circle-stroke-color": "rgba(0, 0, 0, 0.15)"
      }
    });

    // Inner filled circle
    mapInstance.addLayer({
      id: "stations-circle",
      type: "circle",
      source: "stations-data",
      paint: {
        "circle-radius": [
          "case",
          ["boolean", ["get", "isCurrent"], false], 6.5,
          ["boolean", ["get", "isTerminus"], false], 5.5,
          4
        ],
        "circle-color": [
          "case",
          ["boolean", ["get", "isCurrent"], false], "#15803d",
          ["boolean", ["get", "isPassed"], false], "#94a3b8",
          "#1e40af"
        ]
      }
    });

    // Invisible touch buffer layer for finger tapping on mobile (20px radius)
    mapInstance.addLayer({
      id: "stations-touch-target",
      type: "circle",
      source: "stations-data",
      paint: {
        "circle-radius": 20,
        "circle-opacity": 0
      }
    });
  } catch (err) {
    console.warn("[MapLibre] stations layer draw warning:", err);
  }

  // Station click popup listener (registered once per map instance)
  if (!mapInstance._hasStationClickRegistered) {
    mapInstance._hasStationClickRegistered = true;
    const showStationPopup = (e) => {
      const feat = e.features?.[0];
      if (!feat) return;
      const p = feat.properties;
      const coords = feat.geometry.coordinates;

      new window.maplibregl.Popup({ offset: 10 })
        .setLngLat(coords)
        .setHTML(`
          <div style="font-family:sans-serif;min-width:160px;padding:2px;">
            <strong style="font-size:14px;color:#0f172a;">${p.name}${p.code ? ` (${p.code})` : ""}</strong>
            <div style="font-size:12px;color:#64748b;margin-top:4px;">
              ${p.scheduledArr && p.scheduledArr !== "—" ? `Arr: <strong>${p.scheduledArr}</strong>` : ""}
              ${p.scheduledDep && p.scheduledDep !== "—" ? ` · Dep: <strong>${p.scheduledDep}</strong>` : ""}
            </div>
            ${p.platform ? `<div style="font-size:11px;color:#1e40af;margin-top:2px;font-weight:600;">Platform ${p.platform}</div>` : ""}
            <div style="font-size:11px;color:#64748b;margin-top:2px;">${p.distanceKm ? `${p.distanceKm} km from origin` : ""}</div>
          </div>
        `)
        .addTo(mapInstance);
    };

    mapInstance.on("click", "stations-circle", showStationPopup);
    mapInstance.on("click", "stations-touch-target", showStationPopup);

    mapInstance.on("mouseenter", "stations-touch-target", () => {
      mapInstance.getCanvas().style.cursor = "pointer";
    });
    mapInstance.on("mouseleave", "stations-touch-target", () => {
      mapInstance.getCanvas().style.cursor = "";
    });
  }

  // Train marker
  if (pos && !isNaN(pos.lon) && !isNaN(pos.lat)) {
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
    try {
      trainMapMarker = new window.maplibregl.Marker({
        element: trainMarkerEl,
        rotationAlignment: "map",
        pitchAlignment: "map"
      })
        .setLngLat([pos.lon, pos.lat])
        .addTo(mapInstance);
    } catch (e) {}
  }

  // Fit map to route bounds
  const coords = route.routeCoordinates.filter(c => GeoService.isValidIndiaCord(c[0], c[1]));
  if (coords.length > 1) {
    const bounds = coords.reduce(
      (b, c) => b.extend(c),
      new window.maplibregl.LngLatBounds(coords[0], coords[0])
    );
    mapInstance.fitBounds(bounds, { padding: 40, duration: 1100, maxZoom: 9 });
  }

  // Force canvas resize so WebGL buffer matches DOM viewport immediately
  setTimeout(() => {
    if (mapInstance) {
      try { mapInstance.resize(); } catch (e) {}
    }
  }, 80);
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

  // Dynamically update GPU stations GeoJSON without layer teardown
  const sSrc = mapInstance.getSource("stations-data");
  if (sSrc && route.stations?.length) {
    const curIdx = trackerService.getCurrentStationIndex();
    const validStations = (route.stations || []).filter(stn => {
      if (!stn.latitude || !stn.longitude) return false;
      const lat = Number(stn.latitude);
      const lon = Number(stn.longitude);
      return !isNaN(lat) && !isNaN(lon) && GeoService.isValidIndiaCord(lon, lat);
    });

    sSrc.setData({
      type: "FeatureCollection",
      features: validStations.map((stn, idx) => ({
        type: "Feature",
        geometry: { type: "Point", coordinates: [Number(stn.longitude), Number(stn.latitude)] },
        properties: {
          idx,
          name: stn.name,
          code: stn.code,
          scheduledArr: stn.scheduledArr || "—",
          scheduledDep: stn.scheduledDep || "—",
          platform: stn.platform || "",
          distanceKm: stn.distanceKm || 0,
          isPassed: idx < curIdx,
          isCurrent: idx === curIdx,
          isTerminus: idx === 0 || idx === validStations.length - 1
        }
      }))
    });
  }
}

function updateTrainMarker(pos) {
  if (!pos || isNaN(pos.lon) || isNaN(pos.lat) || !trainMapMarker) return;
  trainMapMarker.setLngLat([pos.lon, pos.lat]);
  const inner = trainMarkerEl?.querySelector(".train-marker-inner");
  if (inner) inner.style.transform = `rotate(${pos.bearing || 0}deg)`;
}

function lineFeature(coordinates) {
  return { type: "Feature", properties: {}, geometry: { type: "LineString", coordinates } };
}

/* ── Device Location / User GPS ───────────────────────────── */

export function locateUserOnMap() {
  if (!navigator.geolocation) {
    store.showToast("Geolocation is not supported by your browser");
    return;
  }
  store.showToast("Locating your device… 📍");
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const { latitude, longitude, accuracy } = pos.coords;
      if (!mapInstance) return;

      if (!userLocationMarker) {
        userLocationEl = document.createElement("div");
        userLocationEl.className = "user-location-marker";
        userLocationEl.innerHTML = `
          <div class="user-location-pulse"></div>
          <div class="user-location-dot"></div>
        `;
        userLocationMarker = new window.maplibregl.Marker({ element: userLocationEl })
          .setLngLat([longitude, latitude])
          .addTo(mapInstance);
      } else {
        userLocationMarker.setLngLat([longitude, latitude]);
      }

      // Check nearest station on active route
      const state = store.getState();
      let nearestText = "";
      if (state.liveRoute?.stations?.length) {
        let minDist = Infinity;
        let nearStn = null;
        for (const stn of state.liveRoute.stations) {
          if (stn.latitude && stn.longitude) {
            const d = GeoService.distanceKm(latitude, longitude, stn.latitude, stn.longitude);
            if (d < minDist) { minDist = d; nearStn = stn; }
          }
        }
        if (nearStn && minDist < 200) {
          nearestText = `<div style="font-size:12px;color:#1e40af;margin-top:4px;">📍 <strong>${minDist.toFixed(1)} km</strong> from ${nearStn.name} (${nearStn.code})</div>`;
        }
      }

      const popup = new window.maplibregl.Popup({ offset: 12 }).setHTML(`
        <div style="font-family:sans-serif;min-width:140px;padding:2px;">
          <strong style="font-size:13px;color:#0f172a;">Your Location</strong>
          <div style="font-size:11px;color:#64748b;margin-top:2px;">Accuracy: ~${Math.round(accuracy || 10)}m</div>
          ${nearestText}
        </div>
      `);
      userLocationMarker.setPopup(popup);

      mapInstance.flyTo({ center: [longitude, latitude], zoom: 12, duration: 1400 });
      userLocationMarker.togglePopup();
      store.showToast("Location centered on map 🎯");
    },
    (err) => {
      console.warn("Geolocation error:", err);
      store.showToast("Location access denied — enable GPS permissions");
    },
    { enableHighAccuracy: true, timeout: 8000, maximumAge: 30000 }
  );
}

/* ── Fullscreen Toolbar & Inactivity Timers ───────────────── */

let recenterInactivityTimer = null;
let fullscreenHideTimer     = null;

function hideFullscreenToolbar() {
  if (fullscreenHideTimer) {
    clearTimeout(fullscreenHideTimer);
    fullscreenHideTimer = null;
  }
  const controls = document.getElementById("mapControlsFloating");
  const peekTab  = document.getElementById("mapEdgePeekTab");
  if (controls) controls.classList.add("toolbar-hidden");
  peekTab?.classList.add("visible");
}

function scheduleFullscreenToolbarHide(delay = 3500) {
  if (fullscreenHideTimer) clearTimeout(fullscreenHideTimer);
  const wrapper = document.querySelector(".map-wrapper");
  if (!wrapper?.classList.contains("map-wrapper--fullscreen")) return;
  fullscreenHideTimer = setTimeout(() => {
    hideFullscreenToolbar();
  }, delay);
}

function showFullscreenToolbar(autoHideDelay = 4000) {
  if (fullscreenHideTimer) clearTimeout(fullscreenHideTimer);
  const controls = document.getElementById("mapControlsFloating");
  const peekTab  = document.getElementById("mapEdgePeekTab");
  if (controls) {
    controls.classList.remove("toolbar-hidden");
  }
  peekTab?.classList.remove("visible");
  const wrapper = document.querySelector(".map-wrapper");
  if (wrapper?.classList.contains("map-wrapper--fullscreen")) {
    scheduleFullscreenToolbarHide(autoHideDelay);
  }
}

/* ── Events ───────────────────────────────────────────────── */

export function attachMapEvents() {
  const locateBtn = document.getElementById("mapLocateUserBtn");
  if (locateBtn) {
    locateBtn.addEventListener("click", () => locateUserOnMap());
  }

  const followBtn = document.getElementById("mapFollowToggleBtn");
  if (followBtn) {
    followBtn.addEventListener("click", () => {
      const follow = !store.getState().followTrain;
      store.setFollowTrain(follow);
      followBtn.classList.toggle("active", follow);
      if (follow && mapInstance) {
        const pos = trackerService.getCurrentPosition();
        if (pos && !isNaN(pos.lon) && !isNaN(pos.lat)) {
          mapInstance.easeTo({ center: [pos.lon, pos.lat], zoom: 7 });
          showRecenterBtn(false);
        }
      }
    });
  }

  const recenterBtn = document.getElementById("mapRecenterBtn");
  if (recenterBtn) {
    recenterBtn.addEventListener("click", () => {
      store.setFollowTrain(true);
      const pos = trackerService.getCurrentPosition();
      if (mapInstance && pos && !isNaN(pos.lon) && !isNaN(pos.lat)) {
        mapInstance.flyTo({ center: [pos.lon, pos.lat], zoom: 7 });
      }
      showRecenterBtn(false);
    });
  }

  // Three-dots toggle button for small screen view
  const moreToolsBtn = document.getElementById("mapMoreToolsBtn");
  const floatingControls = document.getElementById("mapControlsFloating");
  if (moreToolsBtn && floatingControls) {
    moreToolsBtn.addEventListener("click", (e) => {
      e.preventDefault();
      e.stopPropagation();
      const isExpanded = floatingControls.classList.toggle("is-expanded");
      moreToolsBtn.classList.toggle("active", isExpanded);
      const dotsIcon = moreToolsBtn.querySelector(".more-tools-icon-dots");
      const closeIcon = moreToolsBtn.querySelector(".more-tools-icon-close");
      if (dotsIcon) dotsIcon.style.display = isExpanded ? "none" : "block";
      if (closeIcon) closeIcon.style.display = isExpanded ? "block" : "none";
    });
  }

  // Dismiss expanded small-screen tools when tapping map canvas
  const mapCanvas = document.getElementById("railMap");
  if (mapCanvas && floatingControls) {
    mapCanvas.addEventListener("click", () => {
      const wrapper = document.querySelector(".map-wrapper");
      if (!wrapper?.classList.contains("map-wrapper--fullscreen")) {
        floatingControls.classList.remove("is-expanded");
        moreToolsBtn?.classList.remove("active");
        const dotsIcon = moreToolsBtn?.querySelector(".more-tools-icon-dots");
        const closeIcon = moreToolsBtn?.querySelector(".more-tools-icon-close");
        if (dotsIcon) dotsIcon.style.display = "block";
        if (closeIcon) closeIcon.style.display = "none";
      }
    });
  }

  // Google Maps-style Fullscreen Right-Edge Peek Tab
  const peekTab = document.getElementById("mapEdgePeekTab");
  if (peekTab) {
    peekTab.addEventListener("click", (e) => {
      e.stopPropagation();
      showFullscreenToolbar(2800);
    });
    peekTab.addEventListener("touchstart", (e) => {
      e.stopPropagation();
      showFullscreenToolbar(2800);
    }, { passive: true });
  }

  // NOTE: Map canvas touches/clicks do NOT open the toolbar.
  // The toolbar is ONLY opened by intentionally tapping the black initializer (#mapEdgePeekTab).
  // Tapping the map canvas in fullscreen dismisses the toolbar back to the compact button.
  const mapWrapper = document.querySelector(".map-wrapper");
  if (mapCanvas && mapWrapper && !mapWrapper._hasEdgeTouchBound) {
    mapWrapper._hasEdgeTouchBound = true;
    mapCanvas.addEventListener("click", () => {
      if (mapWrapper.classList.contains("map-wrapper--fullscreen")) {
        hideFullscreenToolbar();
      }
    });
  }

  // Keep toolbar visible while interacting with floating controls in fullscreen
  if (floatingControls) {
    const keepToolbarAlive = () => {
      const wrapper = document.querySelector(".map-wrapper");
      if (wrapper?.classList.contains("map-wrapper--fullscreen")) {
        showFullscreenToolbar(3000);
      }
    };
    floatingControls.addEventListener("touchstart", keepToolbarAlive, { passive: true });
    floatingControls.addEventListener("click", keepToolbarAlive);
  }

  // Google Maps Compass Reset North
  const compassBtn = document.getElementById("mapCompassBtn");
  if (compassBtn) {
    compassBtn.addEventListener("click", (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (!mapInstance) return;
      isUserInteracting = true;
      mapInstance.resetNorthPitch({ duration: 400 });
      store.showToast("Orientation reset to North 🧭");
    });
  }

  // 3D Perspective Viewpoint Toggle
  const pitchBtn = document.getElementById("mapPitchBtn");
  if (pitchBtn) {
    pitchBtn.addEventListener("click", (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (!mapInstance) return;
      isUserInteracting = true;
      store.setFollowTrain(false);
      showRecenterBtn(true);
      const cur = mapInstance.getPitch();
      const target = cur > 20 ? 0 : 55;
      mapInstance.easeTo({ pitch: target, duration: 400 });
      store.showToast(target > 0 ? "3D Perspective View active 🏙️" : "2D Top-Down View active 🗺️");
    });
  }

  // Rotate Anti-Clockwise (⟲)
  const rotLeftBtn = document.getElementById("mapRotateLeftBtn");
  if (rotLeftBtn) {
    rotLeftBtn.addEventListener("click", (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (!mapInstance) return;
      isUserInteracting = true;
      store.setFollowTrain(false);
      showRecenterBtn(true);
      const b = mapInstance.getBearing();
      mapInstance.easeTo({ bearing: b - 45, duration: 300 });
    });
  }

  // Rotate Clockwise (⟳)
  const rotRightBtn = document.getElementById("mapRotateRightBtn");
  if (rotRightBtn) {
    rotRightBtn.addEventListener("click", (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (!mapInstance) return;
      isUserInteracting = true;
      store.setFollowTrain(false);
      showRecenterBtn(true);
      const b = mapInstance.getBearing();
      mapInstance.easeTo({ bearing: b + 45, duration: 300 });
    });
  }

  const zoomIn  = document.getElementById("mapZoomInBtn");
  const zoomOut = document.getElementById("mapZoomOutBtn");
  if (zoomIn) {
    zoomIn.addEventListener("click", (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (!mapInstance) return;
      isUserInteracting = true;
      store.setFollowTrain(false);
      showRecenterBtn(true);
      mapInstance.easeTo({ zoom: Math.min(mapInstance.getZoom() + 1, 18), duration: 240 });
    });
  }
  if (zoomOut) {
    zoomOut.addEventListener("click", (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (!mapInstance) return;
      isUserInteracting = true;
      store.setFollowTrain(false);
      showRecenterBtn(true);
      mapInstance.easeTo({ zoom: Math.max(mapInstance.getZoom() - 1, 2), duration: 240 });
    });
  }

  const fsBtn = document.getElementById("mapFullscreenBtn");
  if (fsBtn) {
    fsBtn.addEventListener("click", () => {
      toggleMapFullscreen();
    });
  }
}

export function toggleMapFullscreen() {
  const wrapper = document.querySelector(".map-wrapper");
  const fsBtn   = document.getElementById("mapFullscreenBtn");
  if (!wrapper) return;

  const isFs = wrapper.classList.contains("map-wrapper--fullscreen") || !!document.fullscreenElement;

  if (isFs) {
    // Exit fullscreen
    wrapper.classList.remove("map-wrapper--fullscreen");
    document.body.classList.remove("map-fullscreen-active");
    if (document.fullscreenElement) {
      document.exitFullscreen().catch(() => {});
    }
    if (fullscreenHideTimer) clearTimeout(fullscreenHideTimer);
    const controls = document.getElementById("mapControlsFloating");
    const peekTab  = document.getElementById("mapEdgePeekTab");
    controls?.classList.remove("toolbar-hidden");
    peekTab?.classList.remove("visible");

    if (fsBtn) {
      fsBtn.innerHTML = `
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/>
        </svg>
      `;
      fsBtn.title = "Toggle Fullscreen";
      fsBtn.classList.remove("active");
    }
  } else {
    // Enter fullscreen: use CSS class first (100% reliable on iOS Safari), and try native fullscreen on desktop
    wrapper.classList.add("map-wrapper--fullscreen");
    document.body.classList.add("map-fullscreen-active");

    const canNative = typeof wrapper.requestFullscreen === "function" && !/iPhone|iPad|iPod/i.test(navigator.userAgent);
    if (canNative) {
      wrapper.requestFullscreen().catch(() => {});
    }

    if (fsBtn) {
      fsBtn.innerHTML = `
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 14h6v6m10-10h-6V4m0 6 7-7M3 21l7-7"/>
        </svg>
      `;
      fsBtn.title = "Exit Fullscreen";
      fsBtn.classList.add("active");
    }

    // Start fullscreen with a clean, hidden toolbar — user must tap the black initializer to open tools
    hideFullscreenToolbar();

    store.showToast("Map Fullscreen active 🗺️ — tap ◀ button for tools");
  }

  // Force map canvas to resize immediately to match new full-screen dimensions
  setTimeout(() => {
    if (mapInstance) {
      try { mapInstance.resize(); } catch (e) {}
    }
  }, 60);
  setTimeout(() => {
    if (mapInstance) {
      try { mapInstance.resize(); } catch (e) {}
    }
  }, 220);
}

// Global listener for native fullscreen change & escape key
if (!window._mapFullscreenListenersAdded) {
  window._mapFullscreenListenersAdded = true;
  document.addEventListener("fullscreenchange", () => {
    const wrapper = document.querySelector(".map-wrapper");
    const fsBtn   = document.getElementById("mapFullscreenBtn");
    if (!document.fullscreenElement && wrapper?.classList.contains("map-wrapper--fullscreen")) {
      wrapper.classList.remove("map-wrapper--fullscreen");
      document.body.classList.remove("map-fullscreen-active");
      if (fullscreenHideTimer) clearTimeout(fullscreenHideTimer);
      const controls = document.getElementById("mapControlsFloating");
      const peekTab  = document.getElementById("mapEdgePeekTab");
      controls?.classList.remove("toolbar-hidden");
      peekTab?.classList.remove("visible");

      if (fsBtn) {
        fsBtn.innerHTML = `
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/>
          </svg>
        `;
        fsBtn.classList.remove("active");
      }
      setTimeout(() => mapInstance?.resize(), 60);
    }
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      const wrapper = document.querySelector(".map-wrapper");
      if (wrapper?.classList.contains("map-wrapper--fullscreen")) {
        toggleMapFullscreen();
      }
    }
  });
}

function showRecenterBtn(show) {
  const btn = document.getElementById("mapRecenterBtn");
  if (!btn) return;

  if (recenterInactivityTimer) {
    clearTimeout(recenterInactivityTimer);
    recenterInactivityTimer = null;
  }

  if (show) {
    btn.classList.add("visible");
    // Auto-hide when user shows no activity for 3.2 seconds
    recenterInactivityTimer = setTimeout(() => {
      btn.classList.remove("visible");
    }, 3200);
  } else {
    btn.classList.remove("visible");
  }
}

export function focusLocationOnMap(lon, lat, name) {
  if (!mapInstance || isNaN(lon) || isNaN(lat)) return;
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
