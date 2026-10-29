/**
 * RailGaadi Application Bootstrap & Controller — Phase 2 (Mobile-First)
 * 4 tabs: tracking, analytics, companion, board
 * Bottom nav on mobile, segmented control on desktop
 * Dark mode, fav drawer, notification integration
 */

import { store } from "./state/store.js";
import { trackerService } from "./services/trackerService.js";
import { notificationService } from "./services/notificationService.js";
import { renderHeader, attachHeaderEvents } from "./components/header.js";
import { renderSearchSection, attachSearchEvents, updateQuickChips } from "./components/search.js";
import { renderStatusCard, attachStatusCardEvents } from "./components/statusCard.js";
import { renderMapSection, initOrUpdateMap, attachMapEvents } from "./components/maplibreView.js";
import { renderAnalyticsSection, drawElevationChart } from "./components/analyticsView.js";
import { renderCompanionSection, attachCompanionEvents } from "./components/companionView.js";
import { renderShareModal, attachShareModalEvents } from "./components/shareModal.js";
import { renderStationBoard, attachStationBoardEvents } from "./components/stationBoard.js";
import { renderFavDrawer, attachFavDrawerEvents } from "./components/favDrawer.js";

// ── Tab definitions (used for both desktop segmented-control & mobile bottom nav) ──

const TABS = [
  {
    id: "tracking",
    label: "Live Map",
    icon: `<rect width="16" height="16" x="4" y="3" rx="2"/><path d="M4 11h16"/><path d="M12 3v8"/><path d="m8 19-2 3"/><path d="m18 22-2-3"/>`,
  },
  {
    id: "analytics",
    label: "Analytics",
    icon: `<path d="M3 3v18h18"/><path d="m19 9-5 5-4-4-3 3"/>`,
  },
  {
    id: "companion",
    label: "Weather",
    icon: `<circle cx="12" cy="12" r="10"/><path d="m4.93 4.93 4.24 4.24"/><path d="m14.83 14.83 4.24 4.24"/><path d="m14.83 9.17 4.24-4.24"/><path d="m4.93 19.07 4.24-4.24"/>`,
  },
  {
    id: "board",
    label: "Station",
    icon: `<rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/><circle cx="12" cy="16" r="1"/>`,
  },
];

// ── Helpers ────────────────────────────────────────────────────────────────

function setInnerHTML(id, html) {
  const el = document.getElementById(id);
  if (el) el.innerHTML = html;
}

/** Render the mobile bottom navigation bar HTML */
function renderBottomNav() {
  const state = store.getState();
  return `
    <nav class="bottom-nav" id="bottomNav" role="tablist" aria-label="Main navigation">
      ${TABS.map((tab) => `
        <button
          class="bottom-nav-item ${state.activeTab === tab.id ? "active" : ""}"
          data-tab="${tab.id}"
          role="tab"
          aria-selected="${state.activeTab === tab.id}"
          aria-label="${tab.label}"
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            ${tab.icon}
          </svg>
          <span>${tab.label}</span>
        </button>
      `).join("")}
    </nav>
  `;
}

/** Update active state on both segmented-control (desktop) and bottom-nav (mobile) */
function syncTabActiveState(activeTabId) {
  // Desktop segmented control
  document.querySelectorAll(".segmented-btn").forEach((btn) => {
    const isActive = btn.getAttribute("data-tab") === activeTabId;
    btn.classList.toggle("active", isActive);
    btn.setAttribute("aria-selected", String(isActive));
  });
  // Mobile bottom nav
  document.querySelectorAll(".bottom-nav-item").forEach((btn) => {
    const isActive = btn.getAttribute("data-tab") === activeTabId;
    btn.classList.toggle("active", isActive);
    btn.setAttribute("aria-selected", String(isActive));
  });
}

// ── Reactive render ────────────────────────────────────────────────────────

function renderApp(changedKeys = []) {
  const changed = new Set(changedKeys);
  const state   = store.getState();

  // --- Countdown text swap only ---
  if (changedKeys.length === 1 && changed.has("countdownSeconds")) {
    const el = document.getElementById("countdownDisplay");
    if (el) el.textContent = `${state.countdownSeconds}s`;
    return;
  }

  // --- Toast only ---
  if (changedKeys.length === 1 && changed.has("toast")) {
    const tc = document.getElementById("toastContainer");
    if (tc) {
      tc.innerHTML = state.toast
        ? `<div class="toast" role="alert"><span>${state.toast}</span></div>`
        : "";
    }
    return;
  }

  // --- Share modal only ---
  if (changedKeys.length === 1 && changed.has("isShareModalOpen")) {
    setInnerHTML("shareModalRoot", renderShareModal());
    attachShareModalEvents();
    return;
  }

  // --- Fav Drawer only ---
  if (changedKeys.length === 1 && changed.has("isFavDrawerOpen")) {
    setInnerHTML("favDrawerRoot", renderFavDrawer());
    attachFavDrawerEvents();
    return;
  }

  // --- Dark mode toggle — no DOM rebuild needed (CSS handles it) ---
  if (changedKeys.length === 1 && changed.has("darkMode")) {
    const hdrWrapper = document.querySelector(".app-header");
    if (hdrWrapper) {
      const fullHeader = renderHeader();
      const headerHtml = fullHeader.split("\n    <!-- Favourites")[0];
      hdrWrapper.outerHTML = headerHtml;
      attachHeaderEvents();
    }
    return;
  }

  // --- Notification state change — update header bell ---
  if (changedKeys.length === 1 && changed.has("notificationsEnabled")) {
    const hdrWrapper = document.querySelector(".app-header");
    if (hdrWrapper) {
      const fullHeader = renderHeader();
      const headerHtml = fullHeader.split("\n    <!-- Favourites")[0];
      hdrWrapper.outerHTML = headerHtml;
      attachHeaderEvents();
    }
    return;
  }

  // --- Map style only ---
  if (changed.has("mapStyle")) {
    const appCont = document.querySelector(".app-container");
    if (appCont) appCont.setAttribute("data-map-style", state.mapStyle);
    if (state.activeTab === "tracking") {
      initOrUpdateMap();
    }
    const hdrWrapper = document.querySelector(".app-header");
    if (hdrWrapper) {
      const fullHeader = renderHeader();
      const headerHtml = fullHeader.split("\n    <!-- Favourites")[0];
      hdrWrapper.outerHTML = headerHtml;
      attachHeaderEvents();
    }
    return;
  }

  // --- Tab switch ---
  if (changed.has("activeTab")) {
    syncTabActiveState(state.activeTab);
    // Scroll to top on tab switch (mobile UX)
    window.scrollTo({ top: 0, behavior: "smooth" });
    renderViewContainer();
    return;
  }

  // --- Quick chips update on favourites/recents or active train change ---
  if (changed.has("favourites") || changed.has("recentSearches") || changed.has("activeTrainId")) {
    updateQuickChips();
    if (state.isFavDrawerOpen) {
      setInnerHTML("favDrawerRoot", renderFavDrawer());
      attachFavDrawerEvents();
    }
    const favBtn = document.getElementById("headerFavBtn");
    if (favBtn) {
      const fCount = state.favourites.length;
      const badge  = favBtn.querySelector(".fav-count-badge");
      if (badge) badge.textContent = fCount;
    }
  }

  // --- Core views update ---
  const dataKeys = [
    "liveStatus", "liveRoute", "isLoadingStatus", "isLoadingRoute",
    "statusError", "routeError", "activeTrain", "activeTrainId",
    "isStale", "lastUpdatedAt", "favourites", "followTrain",
  ];

  const hasDataChange = dataKeys.some((k) => changed.has(k));

  if (hasDataChange) {
    const status = state.liveStatus;
    if (status?.speedKmh != null) {
      store.recordSpeed(status.speedKmh);
    }

    if (state.notificationsEnabled && status) {
      notificationService.checkAndNotify(status, state.activeTrain);
    }

    if (state.activeTab === "tracking") {
      setInnerHTML("statusCardRoot", renderStatusCard());
      attachStatusCardEvents();
      initOrUpdateMap();
    } else if (state.activeTab === "analytics") {
      const ar = document.getElementById("analyticsViewRoot");
      if (ar) {
        ar.innerHTML = renderAnalyticsSection();
        setTimeout(() => drawElevationChart(), 50);
      }
    } else if (state.activeTab === "companion") {
      const cr = document.getElementById("companionViewRoot");
      if (cr) {
        cr.innerHTML = renderCompanionSection();
        attachCompanionEvents();
      }
    } else if (state.activeTab === "board") {
      const br = document.getElementById("stationBoardRoot");
      if (br) {
        br.innerHTML = renderStationBoard();
        attachStationBoardEvents();
      }
    }
  }
}

// ── Full initial render ────────────────────────────────────────────────────

function fullRender() {
  const root  = document.getElementById("root");
  if (!root) return;
  const state = store.getState();

  root.innerHTML = `
    <div class="app-container" data-map-style="${state.mapStyle}">
      ${renderHeader()}

      <main class="main-content">
        <div id="searchSectionRoot">${renderSearchSection()}</div>

        <!-- Desktop Segmented Tab Navigation (hidden on mobile via CSS) -->
        <div class="view-tabs-container">
          <nav class="segmented-control" role="tablist" aria-label="View Mode">
            ${TABS.map((tab) => `
              <button class="segmented-btn ${state.activeTab === tab.id ? "active" : ""}"
                data-tab="${tab.id}" role="tab"
                aria-selected="${state.activeTab === tab.id}"
                id="tab-${tab.id}">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${tab.icon}</svg>
                <span>${tab.label}</span>
              </button>
            `).join("")}
          </nav>
        </div>

        <!-- View Content Router -->
        <div id="viewContainer"></div>
      </main>

      <!-- Mobile Bottom Navigation (shown on mobile via CSS) -->
      ${renderBottomNav()}

      <!-- Toast -->
      <div class="toast-container" id="toastContainer">
        ${state.toast ? `<div class="toast" role="alert"><span>${state.toast}</span></div>` : ""}
      </div>

      <!-- Share Modal -->
      <div id="shareModalRoot">${renderShareModal()}</div>

      <!-- Favourites Drawer -->
      <div id="favDrawerRoot">${renderFavDrawer()}</div>
    </div>
  `;

  renderViewContainer();
  attachHeaderEvents();
  attachSearchEvents();
  attachShareModalEvents();
  attachFavDrawerEvents();
  attachTabAndViewEvents();
}

function renderViewContainer() {
  const container = document.getElementById("viewContainer");
  if (!container) return;
  const state = store.getState();

  if (state.activeTab === "tracking") {
    container.innerHTML = `
      <div class="workspace-grid">
        <div id="statusCardRoot">${renderStatusCard()}</div>
        <div>${renderMapSection()}</div>
      </div>
    `;
    attachStatusCardEvents();
    attachMapEvents();
    setTimeout(() => initOrUpdateMap(), 80);

  } else if (state.activeTab === "analytics") {
    container.innerHTML = `<div id="analyticsViewRoot">${renderAnalyticsSection()}</div>`;
    setTimeout(() => drawElevationChart(), 80);

  } else if (state.activeTab === "companion") {
    container.innerHTML = `<div id="companionViewRoot">${renderCompanionSection()}</div>`;
    attachCompanionEvents();

  } else if (state.activeTab === "board") {
    container.innerHTML = `<div id="stationBoardRoot">${renderStationBoard()}</div>`;
    attachStationBoardEvents();
  }
}

function attachTabAndViewEvents() {
  // Desktop segmented control
  document.querySelectorAll(".segmented-btn").forEach((btn) => {
    btn.addEventListener("click", () => store.setActiveTab(btn.getAttribute("data-tab")));
  });
  // Mobile bottom nav
  document.querySelectorAll(".bottom-nav-item").forEach((btn) => {
    btn.addEventListener("click", () => store.setActiveTab(btn.getAttribute("data-tab")));
  });
}

/* ── Window resize ───────────────────────────────────────────── */

window.addEventListener("resize", () => {
  if (store.getState().activeTab === "analytics") drawElevationChart();
});

/* ── Deep-link hashchange ───────────────────────────────── */

window.addEventListener("hashchange", () => {
  const params  = new URLSearchParams(window.location.hash.replace("#", "?"));
  const trainId = params.get("train");
  if (trainId && trainId !== store.getState().activeTrainId) {
    trackerService.loadTrain(trainId);
  }
});

/* ── Store Subscription ─────────────────────────────────── */

store.subscribe((state, changedKeys) => renderApp(changedKeys));

/* ── Service Worker Registration (PWA) ──────────────────── */

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("/sw.js")
      .then((reg) => console.log("[RailGaadi] Service Worker registered:", reg.scope))
      .catch((err) => console.log("[RailGaadi] Service Worker registration skipped:", err.message));
  });
}

/* ── Boot ──── */

function boot() {
  fullRender();

  const params  = new URLSearchParams(window.location.hash.replace("#", "?"));
  const trainId = params.get("train");
  if (trainId) {
    trackerService.loadTrain(trainId);
  }
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", boot);
} else {
  boot();
}
