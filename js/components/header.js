/**
 * RailGaadi Header — Clean & Minimalist Mobile Architecture
 * Preserves original brand identity + adds Language Selector Pill,
 * Icon-only Theme Toggle, and Space-Saving Quick Menu (☰)
 */

import { store } from "../state/store.js";
import { renderFavDrawer, attachFavDrawerEvents } from "./favDrawer.js";
import { notificationService } from "../services/notificationService.js";
import { getLanguagePill, getLanguage } from "../i18n/index.js";

let mapStyleMenuOpen = false;

export function renderHeader() {
  const state    = store.getState();
  const favCount = state.favourites.length;
  const isDark   = state.darkMode;
  const hasNotif = state.notificationsEnabled;
  const curStyle = state.mapStyle;
  const curLang  = state.language || getLanguage();
  const langPill = getLanguagePill(curLang);

  const styleLabels = {
    light:     { icon: "☀️",  label: "Light" },
    dark:      { icon: "🌙",  label: "Dark" },
    satellite: { icon: "🛰️", label: "Satellite" },
    terrain:   { icon: "⛰️",  label: "Terrain" }
  };

  return `
    <header class="app-header">
      <!-- Original Brand Identity (Untouched, Clean & Rich) -->
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

      <!-- Header Actions: Minimalist Mobile Layout -->
      <div class="header-actions">

        <!-- 1. Language Selection Pill (Always visible, 1-tap language switch) -->
        <button class="header-btn header-lang-btn" id="headerLangBtn" type="button"
          title="Change Language / भाषा बदलें" aria-label="Language selection, current: ${langPill}">
          <svg class="header-lang-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/><path d="M2 12h20"/>
          </svg>
          <span class="header-lang-text">${langPill}</span>
        </button>

        <!-- 2. Dark/Light Mode Toggle Button (Compact, space-saving icon toggle) -->
        <button class="header-btn btn-icon-only dark-toggle-btn ${isDark ? "active" : ""}" id="headerDarkModeBtn"
          type="button"
          title="${isDark ? "Switch to Light Mode" : "Switch to Dark Mode"}"
          aria-label="Toggle dark mode"
          aria-pressed="${isDark}">
          ${isDark ? `
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/>
              <path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/>
              <path d="M2 12h2"/><path d="M20 12h2"/>
              <path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/>
            </svg>
          ` : `
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/>
            </svg>
          `}
        </button>

        <!-- 3. Mini Bar / Hamburger Menu Trigger (Mobile Primary, houses secondary tools) -->
        <button class="header-btn btn-icon-only header-menu-btn" id="headerMenuBtn"
          type="button"
          title="Menu & Tools"
          aria-label="Open menu and tools"
          aria-expanded="${state.isQuickMenuOpen}">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="4" x2="20" y1="6" y2="6"/>
            <line x1="4" x2="20" y1="12" y2="12"/>
            <line x1="4" x2="20" y1="18" y2="18"/>
          </svg>
          ${favCount > 0 || hasNotif ? `<span class="header-menu-indicator-dot"></span>` : ""}
        </button>

        <!-- Desktop-Only Extended Actions (Hidden on mobile < 768px to prevent overcrowding) -->
        <div class="desktop-only-actions">
          
          <!-- Map Style Picker (Desktop) -->
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

          <!-- Notification Bell (Desktop) -->
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

          <!-- Share Journey Button (Desktop) -->
          <button class="header-btn" id="headerShareBtn" title="Share Journey" aria-label="Share Train Status">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/>
              <polyline points="16 6 12 2 8 6"/>
              <line x1="12" y1="2" x2="12" y2="15"/>
            </svg>
            <span class="header-btn-label">Share</span>
          </button>

          <!-- Favourite Drawer Trigger (Desktop) -->
          <button class="header-btn fav-header-btn ${favCount > 0 ? "has-favs" : ""}" id="headerFavBtn"
            title="${favCount} Favourites" aria-label="View Favourites">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="${favCount > 0 ? "#ef4444" : "none"}" stroke="${favCount > 0 ? "#ef4444" : "currentColor"}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
            </svg>
            ${favCount > 0 ? `<span class="fav-count-badge">${favCount}</span>` : ""}
          </button>

        </div>
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

  // 1. Language pill click → open Language Modal
  const langBtn = document.getElementById("headerLangBtn");
  if (langBtn) {
    langBtn.addEventListener("click", () => {
      store.openLangModal();
    });
  }

  // 2. Dark mode toggle
  const darkBtn = document.getElementById("headerDarkModeBtn");
  if (darkBtn) {
    darkBtn.addEventListener("click", () => store.toggleDarkMode());
  }

  // 3. Mini bar / menu button click → toggle Quick Menu
  const menuBtn = document.getElementById("headerMenuBtn");
  if (menuBtn) {
    menuBtn.addEventListener("click", () => {
      store.toggleQuickMenu();
    });
  }

  // Desktop Map style picker
  const mapStyleBtn = document.getElementById("headerMapStyleBtn");
  if (mapStyleBtn) {
    mapStyleBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      mapStyleMenuOpen = !mapStyleMenuOpen;
      const hdrEl = document.querySelector(".app-header");
      if (hdrEl) {
        hdrEl.outerHTML = renderHeader().split("\n    <!-- Favourites")[0];
        attachHeaderEvents();
      }
    });
  }

  // Desktop Map style options
  document.querySelectorAll(".map-style-option").forEach(btn => {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      const style = btn.getAttribute("data-style");
      store.setMapStyle(style);
      mapStyleMenuOpen = false;
    });
  });

  // Desktop Notification bell
  const notifBtn = document.getElementById("headerNotifBtn");
  if (notifBtn) {
    notifBtn.addEventListener("click", () => notificationService.toggleNotifications());
  }

  // Desktop Share button
  const shareBtn = document.getElementById("headerShareBtn");
  if (shareBtn) {
    shareBtn.addEventListener("click", () => store.openShareModal());
  }

  // Desktop Fav drawer button
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
