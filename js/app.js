/**
 * RailGaadi Application Bootstrap & Controller — Phase 2
 * 4 tabs: tracking, analytics, companion, board
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

function setInnerHTML(id, html) {
  const el = document.getElementById(id);
  if (el) el.innerHTML = html;
}

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
    // Re-render just the header to update the toggle icon
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
    // Refresh header to update style picker icon
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
    document.querySelectorAll(".segmented-btn").forEach(btn => {
      const tab    = btn.getAttribute("data-tab");
      const isActive = tab === state.activeTab;
      btn.classList.toggle("active", isActive);
      btn.setAttribute("aria-selected", String(isActive));
    });
    renderViewContainer();
    return;
  }

  // --- Quick chips update on favourites/recents or active train change ---
  if (changed.has("favourites") || changed.has("recentSearches") || changed.has("activeTrainId")) {
    updateQuickChips();
    // Also refresh fav drawer if it's open
    if (state.isFavDrawerOpen) {
      setInnerHTML("favDrawerRoot", renderFavDrawer());
      attachFavDrawerEvents();
    }
    // Refresh header fav count badge
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
    "isStale", "lastUpdatedAt", "favourites", "followTrain"
  ];

  const hasDataChange = dataKeys.some(k => changed.has(k));

  if (hasDataChange) {
    // Track speed history for analytics chart
    const status = state.liveStatus;
    if (status?.speedKmh != null) {
      store.recordSpeed(status.speedKmh);
    }

    // Fire delay notifications
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

function fullRender() {
  const root  = document.getElementById("root");
  if (!root) return;
  const state = store.getState();

  const tabs = [
    { id: "tracking",  label: "Live Map",    icon: `<rect width="16" height="16" x="4" y="3" rx="2"/><path d="M4 11h16"/><path d="M12 3v8"/><path d="m8 19-2 3"/><path d="m18 22-2-3"/>` },
    { id: "analytics", label: "Analytics",   icon: `<path d="M3 3v18h18"/><path d="m19 9-5 5-4-4-3 3"/>` },
    { id: "companion", label: "Companion",   icon: `<circle cx="12" cy="12" r="10"/><path d="m4.93 4.93 4.24 4.24"/><path d="m14.83 14.83 4.24 4.24"/><path d="m14.83 9.17 4.24-4.24"/><path d="m4.93 19.07 4.24-4.24"/>` },
    { id: "board",     label: "Station",     icon: `<rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/><circle cx="12" cy="16" r="1"/>` }
  ];

  root.innerHTML = `
    <div class="app-container" data-map-style="${state.mapStyle}">
      ${renderHeader()}

      <main class="main-content">
        <div id="searchSectionRoot">${renderSearchSection()}</div>

        <!-- Segmented Tab Navigation -->
        <div class="view-tabs-container">
          <nav class="segmented-control" role="tablist" aria-label="View Mode">
            ${tabs.map(tab => `
              <button class="segmented-btn ${state.activeTab === tab.id ? "active" : ""}"
                data-tab="${tab.id}" role="tab"
                aria-selected="${state.activeTab === tab.id}"
                id="tab-${tab.id}">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">${tab.icon}</svg>
                <span>${tab.label}</span>
              </button>
            `).join("")}
          </nav>
        </div>

        <!-- View Content Router -->
        <div id="viewContainer"></div>
      </main>

      <!-- Toast -->
      <div class="toast-container" id="toastContainer">
        ${state.toast ? `<div class="toast" role="alert"><span>${state.toast}</span></div>` : ""}
      </div>

      <!-- Share Modal -->
      <div id="shareModalRoot">${renderShareModal()}</div>
    </div>
  `;

  renderViewContainer();
  attachHeaderEvents();
  attachSearchEvents();
  attachShareModalEvents();
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
  document.querySelectorAll(".segmented-btn").forEach(btn => {
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
      .then(reg => console.log("[RailGaadi] Service Worker registered:", reg.scope))
      .catch(err => console.log("[RailGaadi] Service Worker registration skipped:", err.message));
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
