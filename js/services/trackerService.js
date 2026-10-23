/**
 * RailGaadi Live Tracker Service — v2
 * Orchestrates RailRadar API calls, auto-refresh with backoff, and visibility awareness
 */

import { store } from "../state/store.js";
import {
  searchTrainsAPI,
  getTrainLiveAPI,
  invalidateStatusCache
} from "./railradarService.js";
import { GeoService } from "./geoService.js";
import { notificationService } from "./notificationService.js";

class TrackerService {
  constructor() {
    this.countdownTimer = null;
    this.backoffCount   = 0;
    this.isRefreshing   = false;
    this.initVisibilityListener();
    this.startAutoRefresh();
  }

  /* ─────────────── SEARCH ─────────────── */

  /**
   * Async search via RailRadar API.
   * Returns normalized results. Never throws — returns [] on error.
   */
  async searchTrains(query) {
    if (!query || typeof query !== "string" || query.trim().length < 2) return [];
    return searchTrainsAPI(query.trim());
  }

  /* ─────────────── LOAD TRAIN ─────────────── */

  /**
   * Called when a user selects a train from search results or quick chips.
   * Fetches comprehensive live status + route + geometry in parallel.
   */
  async loadTrain(trainId, meta = null) {
    if (!trainId) return;
    const strId = String(trainId).trim();

    const initialMeta = meta || store.getTrainMeta(strId) || {
      trainId: strId,
      number: strId,
      name: `Train #${strId}`,
      origin: "",
      destination: "",
      type: "Express"
    };

    store.setActiveTrain(strId, initialMeta);
    store.setState({
      isLoadingStatus: true,
      isLoadingRoute: true,
      statusError: null,
      routeError: null
    });

    const date = store.getState().activeDate;

    try {
      const data = await getTrainLiveAPI(strId, date, initialMeta);

      if (data) {
        if (data.status) store.setLiveStatus(data.status);
        if (data.route)  store.setLiveRoute(data.route);
        if (data.train) {
          const merged = { ...initialMeta, ...data.train };
          store.setState({ activeTrain: merged });
          if (data.status) notificationService.checkAndNotify(data.status, merged);
        } else if (data.status) {
          notificationService.checkAndNotify(data.status, initialMeta);
        }
        this.backoffCount = 0;
      }
    } catch (err) {
      console.error("[TrackerService] loadTrain failed:", err);
      this.backoffCount = Math.min(this.backoffCount + 1, 4);
      store.setStatusError("Unable to fetch live train details — click Retry below");
    } finally {
      store.setState({ isLoadingStatus: false, isLoadingRoute: false });
    }
  }

  /* ─────────────── STATUS REFRESH ─────────────── */

  async fetchStatus(trainId, date) {
    const state = store.getState();
    const id = trainId || state.activeTrainId;
    if (!id) return;

    store.setState({ isLoadingStatus: true, statusError: null });
    try {
      const data = await getTrainLiveAPI(id, date || state.activeDate, state.activeTrain);
      if (data?.status) {
        store.setLiveStatus(data.status);
        notificationService.checkAndNotify(data.status, state.activeTrain);
        if (data.route && !state.liveRoute) store.setLiveRoute(data.route);
        this.backoffCount = 0;
      }
    } catch (err) {
      this.backoffCount = Math.min(this.backoffCount + 1, 4);
      store.setStatusError("Telemetry update delayed");
    } finally {
      store.setState({ isLoadingStatus: false });
    }
  }

  async fetchRoute(trainId, date) {
    const state = store.getState();
    const id = trainId || state.activeTrainId;
    if (!id || state.liveRoute) return;

    store.setState({ isLoadingRoute: true });
    try {
      const data = await getTrainLiveAPI(id, date || state.activeDate);
      if (data?.route) store.setLiveRoute(data.route);
    } catch (err) {
      store.setRouteError("Route geometry unavailable");
    } finally {
      store.setState({ isLoadingRoute: false });
    }
  }

  /* ─────────────── DERIVED METRICS ─────────────── */

  /**
   * Computes progress percentage from live status + route geometry.
   * Returns clamped 0–100.
   */
  getProgressPercent() {
    const { liveStatus, liveRoute } = store.getState();
    if (!liveStatus || !liveRoute) return null;

    const { distanceCoveredKm } = liveStatus;
    const total = liveRoute.totalDistanceKm;

    if (distanceCoveredKm != null && total && total > 0) {
      return Math.min(100, Math.max(0, Math.round((distanceCoveredKm / total) * 100)));
    }

    // Fallback: proportion by station index
    if (liveStatus.currentStation && liveRoute.stations?.length > 0) {
      const curCode = liveStatus.currentStation.stationId || liveStatus.currentStation.code;
      const idx = liveRoute.stations.findIndex(s => s.stationId === curCode || s.code === curCode);
      if (idx >= 0) {
        const stn = liveRoute.stations[idx];
        if (stn.distanceKm != null && total && total > 0) {
          return Math.min(100, Math.max(0, Math.round((stn.distanceKm / total) * 100)));
        }
        return Math.round((idx / Math.max(1, liveRoute.stations.length - 1)) * 100);
      }
    }

    return null;
  }

  /**
   * Returns interpolated train position [lon, lat] along route polyline
   */
  getCurrentPosition() {
    const { liveStatus, liveRoute } = store.getState();
    if (!liveRoute?.routeCoordinates?.length) return null;

    // Prefer live GPS from provider
    if (liveStatus?.position?.latitude && liveStatus?.position?.longitude) {
      const { latitude, longitude } = liveStatus.position;
      const bearing = GeoService.bearingAlongRoute(liveRoute.routeCoordinates, longitude, latitude);
      return { lat: latitude, lon: longitude, bearing };
    }

    // Fallback: interpolate from progress %
    const pct = this.getProgressPercent();
    if (pct == null) {
      const first = liveRoute.routeCoordinates[0];
      return first ? { lat: first[1], lon: first[0], bearing: 0 } : null;
    }
    return GeoService.interpolatePosition(liveRoute.routeCoordinates, pct);
  }

  /**
   * Get current station index in liveRoute.stations
   */
  getCurrentStationIndex() {
    const { liveStatus, liveRoute } = store.getState();
    if (!liveStatus?.currentStation || !liveRoute?.stations?.length) return 0;
    const code = liveStatus.currentStation.stationId || liveStatus.currentStation.code;
    const idx  = liveRoute.stations.findIndex(s => s.stationId === code || s.code === code);
    return idx >= 0 ? idx : 0;
  }

  /* ─────────────── AUTO-REFRESH ─────────────── */

  startAutoRefresh() {
    this.stopAutoRefresh();
    this.countdownTimer = setInterval(() => {
      const state = store.getState();
      if (!state.autoRefresh || !state.activeTrainId) return;

      if (state.countdownSeconds <= 1) {
        this.triggerRefresh();
        const nextInterval = state.refreshInterval * Math.pow(1.5, this.backoffCount);
        const capped = Math.min(nextInterval, 180);
        store.setState({ countdownSeconds: Math.round(capped) });
      } else {
        store.setState({ countdownSeconds: state.countdownSeconds - 1 });
      }
    }, 1000);
  }

  stopAutoRefresh() {
    if (this.countdownTimer) clearInterval(this.countdownTimer);
  }

  triggerRefresh() {
    if (this.isRefreshing) return;
    const state = store.getState();
    if (!state.activeTrainId) return;
    this.isRefreshing = true;
    invalidateStatusCache(state.activeTrainId, state.activeDate);
    this.fetchStatus(state.activeTrainId, state.activeDate)
      .finally(() => { this.isRefreshing = false; });
  }

  initVisibilityListener() {
    document.addEventListener("visibilitychange", () => {
      if (document.hidden) {
        this.stopAutoRefresh();
      } else {
        this.triggerRefresh();
        this.startAutoRefresh();
      }
    });
  }
}

export const trackerService = new TrackerService();
