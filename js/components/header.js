/**
 * RailGaadi Header — Phase 2
 * Dark-mode toggle, map-style picker, notification bell, fav drawer
 */

import { store } from "../state/store.js";
import { renderFavDrawer, attachFavDrawerEvents } from "./favDrawer.js";
import { notificationService } from "../services/notificationService.js";

let mapStyleMenuOpen = false;

export function renderHeader() {
  const state    = store.getState();
  const favCount = state.favourites.length;
  const isDark   = state.darkMode;
  const hasNotif = state.notificationsEnabled;
  const curStyle = state.mapStyle;

  const styleLabels = {
    light:     { icon: "☀️",  label: "Light" },
    dark:      { icon: "🌙",  label: "Dark" },
    satellite: { icon: "🛰️", label: "Satellite" },
    terrain:   { icon: "⛰️",  label: "Terrain" }
  };

  return `
    <header class="app-header">
      <div class="brand-wrapper" id="brandLogoBtn" role="button" tabindex="0" aria-label="RailGaadi Home">
        <div class="brand-logo">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <rect width="16" height="16" x="4" y="3" rx="2"/>
            <path d="M4 11h16"/>
            <path d="M12 3v8"/>
            <path d="m8 19-2 3"/>
            <path d="m18 22-2-3"/>
            <circle cx="8" cy="15" r="1"/>
            <circle cx="16" cy="15" r="1"/>
          </svg>
        </div>
        <div class="brand-text">
          <h1>RailGaadi</h1>
        </div>
      </div>

      <div class="header-actions">

        <!-- Map Style Picker -->
        <div class="map-style-picker-wrap" id="mapStylePickerWrap">
          <button class="header-btn map-style-toggle-btn" id="headerMapStyleBtn"
            title="Map Style: ${styleLabels[curStyle]?.label || 'Light'}"
            aria-label="Change map style"
            aria-haspopup="true"
            aria-expanded="${mapStyleMenuOpen}">
            <span>${styleLabels[curStyle]?.icon || "☀️"}</span>
            <span class="map-style-label-text">${styleLabels[curStyle]?.label || "Light"}</span>
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="m6 9 6 6 6-6"/>
            </svg>
          </button>
          ${mapStyleMenuOpen ? `
            <div class="map-style-dropdown" role="menu" id="mapStyleDropdown">
              ${Object.entries(styleLabels).map(([key, val]) => `
                <button class="map-style-option ${curStyle === key ? "active" : ""}"
                  data-style="${key}" role="menuitem">
                  <span>${val.icon}</span>
                  <span>${val.label}</span>
                  ${curStyle === key ? `<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>` : ""}
                </button>
              `).join("")}
            </div>
          ` : ""}
        </div>

        <!-- Dark Mode Toggle -->
        <button class="header-btn dark-toggle-btn ${isDark ? "active" : ""}" id="headerDarkModeBtn"
          title="${isDark ? "Switch to Light Mode" : "Switch to Dark Mode"}"
          aria-label="Toggle dark mode"
          aria-pressed="${isDark}">
          ${isDark ? `
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="4"/>
              <path d="M12 2v2"/><path d="M12 20v2"/>
              <path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/>
              <path d="M2 12h2"/><path d="M20 12h2"/>
              <path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/>
            </svg>
          ` : `
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/>
            </svg>
          `}
          <span class="header-btn-label">${isDark ? "Light" : "Dark"}</span>
        </button>

        <!-- Notification Bell -->
        <button class="header-btn btn-icon-only notif-btn ${hasNotif ? "active" : ""}"
          id="headerNotifBtn"
          title="${hasNotif ? "Notifications Active" : "Enable Delay Alerts"}"
          aria-label="Toggle delay notifications"
          aria-pressed="${hasNotif}">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="${hasNotif ? "currentColor" : "none"}" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/>
            <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>
          </svg>
          ${hasNotif ? `<span class="notif-active-dot"></span>` : ""}
        </button>

        <!-- Share Journey Button -->
        <button class="header-btn" id="headerShareBtn" title="Share Journey" aria-label="Share Train Status">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/>
            <polyline points="16 6 12 2 8 6"/>
            <line x1="12" y1="2" x2="12" y2="15"/>
          </svg>
          <span class="header-btn-label">Share</span>
        </button>

        <!-- Favourite Drawer Trigger -->
        <button class="header-btn fav-header-btn ${favCount > 0 ? "has-favs" : ""}" id="headerFavBtn"
          title="${favCount} Favourites" aria-label="View Favourites">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="${favCount > 0 ? "#f59e0b" : "none"}" stroke="${favCount > 0 ? "#f59e0b" : "currentColor"}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
          </svg>
          ${favCount > 0 ? `<span class="fav-count-badge">${favCount}</span>` : ""}
        </button>
      </div>
    </header>

    <!-- Favourites Drawer -->
    <div id="favDrawerRoot">${renderFavDrawer()}</div>
  `;
}

export function attachHeaderEvents() {
  // Brand logo → go to tracking tab
  const brandBtn = document.getElementById("brandLogoBtn");
  if (brandBtn) {
    brandBtn.addEventListener("click", () => store.setActiveTab("tracking"));
    brandBtn.addEventListener("keydown", e => { if (e.key === "Enter") store.setActiveTab("tracking"); });
  }

  // Map style picker
  const mapStyleBtn = document.getElementById("headerMapStyleBtn");
  if (mapStyleBtn) {
    mapStyleBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      mapStyleMenuOpen = !mapStyleMenuOpen;
      const root = document.getElementById("root");
      if (root) {
        // Re-render just the header section
        const hdrEl = document.querySelector(".app-header");
        const favRoot = document.getElementById("favDrawerRoot");
        if (hdrEl) {
          hdrEl.outerHTML = renderHeader().split("\n    <!-- Favourites")[0];
          attachHeaderEvents();
        }
      }
    });
  }

  // Map style options
  document.querySelectorAll(".map-style-option").forEach(btn => {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      const style = btn.getAttribute("data-style");
      store.setMapStyle(style);
      mapStyleMenuOpen = false;
    });
  });

  // Close map style menu on outside click
  document.addEventListener("click", (e) => {
    if (mapStyleMenuOpen && !e.target.closest("#mapStylePickerWrap")) {
      mapStyleMenuOpen = false;
    }
  }, { once: true });

  // Dark mode toggle
  const darkBtn = document.getElementById("headerDarkModeBtn");
  if (darkBtn) {
    darkBtn.addEventListener("click", () => store.toggleDarkMode());
  }

  // Notification bell
  const notifBtn = document.getElementById("headerNotifBtn");
  if (notifBtn) {
    notifBtn.addEventListener("click", () => notificationService.toggleNotifications());
  }

  // Share button
  const shareBtn = document.getElementById("headerShareBtn");
  if (shareBtn) {
    shareBtn.addEventListener("click", () => store.openShareModal());
  }

  // Fav drawer button
  const favBtn = document.getElementById("headerFavBtn");
  if (favBtn) {
    favBtn.addEventListener("click", () => {
      if (store.getState().isFavDrawerOpen) {
        store.closeFavDrawer();
      } else {
        store.openFavDrawer();
      }
    });
  }

  // Attach fav drawer events
  attachFavDrawerEvents();
}
