/**
 * RailGaadi Reactive State Management — v2
 * Extended to carry async loading, error states, and live train data objects
 */

const STORAGE_KEY_FAVS     = "railgaadi_favs_v1";
const STORAGE_KEY_RECENTS  = "railgaadi_recents_v1";
const STORAGE_KEY_SETTINGS = "railgaadi_settings_v1";

// Map of trainId → { number, name, origin, destination, type } for recently seen trains
const trainMetaCache = new Map();

// Speed history tracking (per session)
const speedHistory = [];

class Store {
  constructor() {
    this.listeners = new Set();

    const savedFavs     = this.loadJSON(STORAGE_KEY_FAVS,     []);
    const savedRecents  = this.loadJSON(STORAGE_KEY_RECENTS,  []);
    const savedSettings = this.loadJSON(STORAGE_KEY_SETTINGS, {
      mapStyle: "light", refreshInterval: 30, autoRefresh: true, darkMode: false
    });

    const params       = new URLSearchParams(window.location.hash.replace("#", "?"));
    const trainFromUrl = params.get("train") || null;

    this.state = {
      /* navigation */
      activeTrainId:   trainFromUrl,
      activeDate:      params.get("date") || new Date().toISOString().split("T")[0],
      activeTab:       "tracking",

      /* live data — populated by trackerService */
      liveStatus:      null,   // TrainStatus object
      liveRoute:       null,   // { stations, routeCoordinates, totalDistanceKm }
      activeTrain:     null,   // { trainId, number, name, origin, destination, type }

      /* async loading states */
      isLoadingStatus: false,
      isLoadingRoute:  false,
      statusError:     null,
      routeError:      null,
      searchResults:   [],
      isSearchLoading: false,
      searchQuery:     "",

      /* map */
      mapStyle:        savedSettings.mapStyle    || "light",
      followTrain:     true,

      /* refresh engine */
      autoRefresh:     savedSettings.autoRefresh !== false,
      refreshInterval: savedSettings.refreshInterval || 30,
      countdownSeconds: savedSettings.refreshInterval || 30,
      lastUpdatedAt:   new Date(),
      isStale:         false,

      /* user data */
      favourites:      savedFavs,
      recentSearches:  savedRecents,

      /* ui helpers */
      selectedPoi:     null,
      isShareModalOpen: false,
      isFavDrawerOpen:  false,
      toast:           null,

      /* app theme */
      darkMode:        savedSettings.darkMode || false,

      /* notification system */
      notificationsEnabled: false,
      delayNotifThreshold: 15 // notify when delay > 15 min
    };

    // Re-hydrate meta cache from recents/favs stored data
    const metaStore = this.loadJSON("railgaadi_train_meta_v1", {});
    Object.entries(metaStore).forEach(([id, meta]) => trainMetaCache.set(id, meta));

    // Apply saved dark mode on init
    if (this.state.darkMode) {
      document.documentElement.setAttribute("data-theme", "dark");
    }
  }

  /* ---- persistence ---- */
  loadJSON(key, fallback) {
    try { const v = localStorage.getItem(key); return v ? JSON.parse(v) : fallback; }
    catch { return fallback; }
  }

  saveJSON(key, value) {
    try { localStorage.setItem(key, JSON.stringify(value)); }
    catch (e) { console.warn("localStorage save failed", e); }
  }

  /* ---- train meta cache (names etc. for chips) ---- */
  cacheTrainMeta(trainId, meta) {
    trainMetaCache.set(trainId, meta);
    const obj = {};
    trainMetaCache.forEach((v, k) => { obj[k] = v; });
    this.saveJSON("railgaadi_train_meta_v1", obj);
  }

  getTrainMeta(trainId) {
    return trainMetaCache.get(trainId) || null;
  }

  /* ---- core ---- */
  getState()   { return this.state; }

  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  notify(changedKeys = []) {
    this.listeners.forEach(l => { try { l(this.state, changedKeys); } catch (e) { console.error(e); } });
  }

  setState(updates) {
    this.state = { ...this.state, ...updates };
    this.notify(Object.keys(updates));
  }

  /* ---- train selection ---- */
  setActiveTrain(trainId, meta = null) {
    if (meta) this.cacheTrainMeta(trainId, meta);
    this.addRecentSearch(trainId);

    this.setState({
      activeTrainId:   trainId,
      liveStatus:      null,
      liveRoute:       null,
      activeTrain:     meta || this.getTrainMeta(trainId),
      isLoadingStatus: true,
      isLoadingRoute:  true,
      statusError:     null,
      routeError:      null,
      countdownSeconds: this.state.refreshInterval,
      isStale:         false,
      selectedPoi:     null,
      lastUpdatedAt:   new Date()
    });

    this.syncUrlHash();
  }

  setLiveStatus(status) {
    this.setState({ liveStatus: status, isLoadingStatus: false, statusError: null, lastUpdatedAt: new Date(), isStale: false });
  }

  setLiveRoute(route) {
    this.setState({ liveRoute: route, isLoadingRoute: false, routeError: null });
  }

  setStatusError(msg) {
    this.setState({ isLoadingStatus: false, statusError: msg, isStale: true });
  }

  setRouteError(msg) {
    this.setState({ isLoadingRoute: false, routeError: msg });
  }

  setActiveTab(tab)   { this.setState({ activeTab: tab }); }
  setMapStyle(style)  {
    this.setState({ mapStyle: style });
    this.saveJSON(STORAGE_KEY_SETTINGS, {
      mapStyle: style,
      refreshInterval: this.state.refreshInterval,
      autoRefresh: this.state.autoRefresh,
      darkMode: this.state.darkMode
    });
  }
  setFollowTrain(f)   { this.setState({ followTrain: f }); }

  toggleDarkMode() {
    const darkMode = !this.state.darkMode;
    if (darkMode) {
      document.documentElement.setAttribute("data-theme", "dark");
    } else {
      document.documentElement.removeAttribute("data-theme");
    }
    this.setState({ darkMode });
    this.saveJSON(STORAGE_KEY_SETTINGS, {
      mapStyle: this.state.mapStyle,
      refreshInterval: this.state.refreshInterval,
      autoRefresh: this.state.autoRefresh,
      darkMode
    });
  }

  openFavDrawer()  { this.setState({ isFavDrawerOpen: true }); }
  closeFavDrawer() { this.setState({ isFavDrawerOpen: false }); }

  setNotificationsEnabled(enabled) { this.setState({ notificationsEnabled: enabled }); }

  // Track speed history for analytics
  recordSpeed(speedKmh) {
    if (speedKmh == null) return;
    speedHistory.push({ t: Date.now(), v: speedKmh });
    if (speedHistory.length > 30) speedHistory.shift(); // keep last 30 readings
  }

  getSpeedHistory() { return [...speedHistory]; }

  toggleFavourite(trainId) {
    const exists = this.state.favourites.includes(trainId);
    const next   = exists ? this.state.favourites.filter(id => id !== trainId) : [...this.state.favourites, trainId];
    this.saveJSON(STORAGE_KEY_FAVS, next);
    this.setState({ favourites: next });
    const meta = this.getTrainMeta(trainId);
    const name = meta?.name || `#${trainId}`;
    this.showToast(exists ? `Removed ${name} from favourites` : `Saved ${name} to favourites ⭐`);
  }

  addRecentSearch(trainId) {
    const list    = this.state.recentSearches.filter(id => id !== trainId);
    const trimmed = [trainId, ...list].slice(0, 8);
    this.saveJSON(STORAGE_KEY_RECENTS, trimmed);
    this.setState({ recentSearches: trimmed });
  }

  showToast(msg, ms = 3500) {
    this.setState({ toast: msg });
    setTimeout(() => { if (this.state.toast === msg) this.setState({ toast: null }); }, ms);
  }

  openShareModal()  { this.setState({ isShareModalOpen: true }); }
  closeShareModal() { this.setState({ isShareModalOpen: false }); }
  selectPoi(poi)    { this.setState({ selectedPoi: poi }); }

  syncUrlHash() {
    const { activeTrainId, activeDate } = this.state;
    if (activeTrainId) window.history.replaceState(null, "", `#train=${activeTrainId}&date=${activeDate}`);
  }
}

export const store = new Store();
