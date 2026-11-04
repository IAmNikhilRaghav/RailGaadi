/**
 * RailGaadi Quick Menu & Language Selector — Mobile-Optimized
 * Provides space-saving overflow drawer and Indian language selector sheet
 */

import { store } from "../state/store.js";
import { SUPPORTED_LANGUAGES, t, getLanguage, getLanguagePill } from "../i18n/index.js";
import { notificationService } from "../services/notificationService.js";

export function renderQuickMenu() {
  const state = store.getState();
  if (!state.isQuickMenuOpen) return "";

  const isDark   = state.darkMode;
  const hasNotif = state.notificationsEnabled;
  const favCount = state.favourites.length;
  const curStyle = state.mapStyle;
  const curLang  = state.language || getLanguage();
  const curLangObj = SUPPORTED_LANGUAGES.find(l => l.code === curLang) || SUPPORTED_LANGUAGES[0];

  return `
    <div class="rg-drawer-backdrop" id="quickMenuBackdrop" role="presentation"></div>
    <aside class="rg-menu-sheet" id="quickMenuSheet" role="dialog" aria-modal="true" aria-label="Quick Actions & Tools">
      
      <!-- Sheet Header -->
      <div class="rg-sheet-header">
        <div class="rg-sheet-header-title">
          <div class="rg-sheet-brand-dot"></div>
          <h2>${t("menu_title", "Menu & Tools")}</h2>
        </div>
        <button class="rg-sheet-close-btn" id="closeQuickMenuBtn" aria-label="Close menu">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 6 6 18"/><path d="m6 6 12 12"/>
          </svg>
        </button>
      </div>

      <!-- Sheet Body -->
      <div class="rg-sheet-body">

        <!-- Appearance (Dark/Light toggle switch) -->
        <div class="rg-menu-item">
          <div class="rg-menu-item-info">
            <div class="rg-menu-item-icon ${isDark ? "is-dark" : "is-light"}">
              ${isDark ? `
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/>
                </svg>
              ` : `
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/>
                </svg>
              `}
            </div>
            <div>
              <div class="rg-menu-item-title">${t("dark_mode", "Dark Mode")}</div>
              <div class="rg-menu-item-sub">${t("dark_mode_sub", "Comfortable viewing at night")}</div>
            </div>
          </div>
          <label class="rg-toggle-switch" aria-label="Toggle dark mode">
            <input type="checkbox" id="menuDarkModeSwitch" ${isDark ? "checked" : ""}>
            <span class="rg-toggle-slider"></span>
          </label>
        </div>

        <!-- Live Delay Alerts toggle switch -->
        <div class="rg-menu-item">
          <div class="rg-menu-item-info">
            <div class="rg-menu-item-icon ${hasNotif ? "is-active-notif" : ""}">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="${hasNotif ? "currentColor" : "none"}" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>
              </svg>
            </div>
            <div>
              <div class="rg-menu-item-title">${t("delay_alerts", "Trip Delay Alerts")}</div>
              <div class="rg-menu-item-sub">${hasNotif ? "Notifications enabled" : t("delay_alerts_sub", "Live notifications on delay")}</div>
            </div>
          </div>
          <label class="rg-toggle-switch" aria-label="Toggle delay notifications">
            <input type="checkbox" id="menuNotifSwitch" ${hasNotif ? "checked" : ""}>
            <span class="rg-toggle-slider"></span>
          </label>
        </div>

        <!-- Language Selector trigger -->
        <button class="rg-menu-item rg-menu-clickable" id="menuLangBtn" type="button">
          <div class="rg-menu-item-info">
            <div class="rg-menu-item-icon is-globe">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/><path d="M2 12h20"/>
              </svg>
            </div>
            <div>
              <div class="rg-menu-item-title">${t("select_language", "Language / भाषा")}</div>
              <div class="rg-menu-item-sub">${curLangObj.native} (${curLangObj.label})</div>
            </div>
          </div>
          <div class="rg-menu-item-tail">
            <span class="rg-lang-badge">${curLangObj.pill}</span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m9 18 6-6-6-6"/>
            </svg>
          </div>
        </button>

        <!-- Saved Trains trigger -->
        <button class="rg-menu-item rg-menu-clickable" id="menuFavTriggerBtn" type="button">
          <div class="rg-menu-item-info">
            <div class="rg-menu-item-icon is-fav">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="${favCount > 0 ? "#ef4444" : "none"}" stroke="${favCount > 0 ? "#ef4444" : "currentColor"}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
              </svg>
            </div>
            <div>
              <div class="rg-menu-item-title">${t("saved_trains", "Saved Trains & Routes")}</div>
              <div class="rg-menu-item-sub">${favCount > 0 ? `${favCount} trains in your saved list` : t("saved_trains_sub", "Quick access to pinned trains")}</div>
            </div>
          </div>
          <div class="rg-menu-item-tail">
            ${favCount > 0 ? `<span class="rg-count-chip">${favCount}</span>` : ""}
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m9 18 6-6-6-6"/>
            </svg>
          </div>
        </button>

        <!-- Share Live Journey trigger -->
        <button class="rg-menu-item rg-menu-clickable" id="menuShareTriggerBtn" type="button">
          <div class="rg-menu-item-info">
            <div class="rg-menu-item-icon is-share">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/><polyline points="16 6 12 2 8 6"/><line x1="12" y1="2" x2="12" y2="15"/>
              </svg>
            </div>
            <div>
              <div class="rg-menu-item-title">${t("share_status", "Share Train Status")}</div>
              <div class="rg-menu-item-sub">${t("share_status_sub", "Send live train link via WhatsApp")}</div>
            </div>
          </div>
          <div class="rg-menu-item-tail">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m9 18 6-6-6-6"/>
            </svg>
          </div>
        </button>

        <!-- Map Style Section -->
        <div class="rg-menu-section">
          <div class="rg-menu-section-header">
            <span>🗺️ ${t("map_style", "Map Style")}</span>
          </div>
          <div class="rg-map-chips-grid">
            ${[
              { key: "light",     icon: "☀️",  label: t("map_light", "Light") },
              { key: "dark",      icon: "🌙",  label: t("map_dark", "Dark") },
              { key: "satellite", icon: "🛰️", label: t("map_satellite", "Satellite") },
              { key: "terrain",   icon: "⛰️",  label: t("map_terrain", "Terrain") }
            ].map(m => `
              <button class="rg-map-chip ${curStyle === m.key ? "is-active" : ""}" data-map-style="${m.key}" type="button">
                <span class="rg-chip-icon">${m.icon}</span>
                <span class="rg-chip-label">${m.label}</span>
              </button>
            `).join("")}
          </div>
        </div>

      </div>
    </aside>
  `;
}

export function attachQuickMenuEvents() {
  const backdrop = document.getElementById("quickMenuBackdrop");
  const closeBtn = document.getElementById("closeQuickMenuBtn");

  backdrop?.addEventListener("click", () => store.closeQuickMenu());
  closeBtn?.addEventListener("click", () => store.closeQuickMenu());

  // Dark mode switch
  const darkSwitch = document.getElementById("menuDarkModeSwitch");
  darkSwitch?.addEventListener("change", () => {
    store.toggleDarkMode();
  });

  // Delay alerts switch
  const notifSwitch = document.getElementById("menuNotifSwitch");
  notifSwitch?.addEventListener("change", () => {
    notificationService.toggleNotifications();
  });

  // Language trigger → closes quick menu and opens language modal
  const langBtn = document.getElementById("menuLangBtn");
  langBtn?.addEventListener("click", () => {
    store.closeQuickMenu();
    setTimeout(() => store.openLangModal(), 80);
  });

  // Saved trains trigger → closes quick menu and opens fav drawer
  const favBtn = document.getElementById("menuFavTriggerBtn");
  favBtn?.addEventListener("click", () => {
    store.closeQuickMenu();
    setTimeout(() => store.openFavDrawer(), 80);
  });

  // Share trigger → closes quick menu and opens share modal
  const shareBtn = document.getElementById("menuShareTriggerBtn");
  shareBtn?.addEventListener("click", () => {
    store.closeQuickMenu();
    setTimeout(() => store.openShareModal(), 80);
  });

  // Map chips
  document.querySelectorAll(".rg-map-chip").forEach(chip => {
    chip.addEventListener("click", () => {
      const style = chip.getAttribute("data-map-style");
      if (style) {
        store.setMapStyle(style);
      }
    });
  });
}

export function renderLangModal() {
  const state = store.getState();
  if (!state.isLangModalOpen) return "";

  const activeLang = state.language || getLanguage();

  return `
    <div class="rg-drawer-backdrop" id="langModalBackdrop" role="presentation"></div>
    <div class="rg-lang-dialog" id="langModalDialog" role="dialog" aria-modal="true" aria-labelledby="langDialogTitle">
      <div class="rg-sheet-header">
        <div class="rg-sheet-header-title">
          <span style="font-size:20px;">🌐</span>
          <div>
            <h2 id="langDialogTitle" style="font-size:var(--text-md);font-weight:700;margin:0;">Select Language</h2>
            <div style="font-size:var(--text-xs);color:var(--color-text-tertiary);">भाषा चुनें • உங்கள் மொழியைத் தேர்வுசெய்க</div>
          </div>
        </div>
        <button class="rg-sheet-close-btn" id="closeLangModalBtn" aria-label="Close language selector">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 6 6 18"/><path d="m6 6 12 12"/>
          </svg>
        </button>
      </div>

      <div class="rg-lang-grid">
        ${SUPPORTED_LANGUAGES.map(lang => {
          const isSelected = lang.code === activeLang;
          return `
            <button class="rg-lang-card ${isSelected ? "is-selected" : ""}" data-lang-code="${lang.code}" type="button">
              <div class="rg-lang-card-main">
                <div class="rg-lang-native">${lang.native}</div>
                <div class="rg-lang-label">${lang.label}</div>
              </div>
              <div class="rg-lang-card-indicator">
                ${isSelected ? `
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--color-accent, #2563eb)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                ` : `
                  <span class="rg-lang-card-pill">${lang.pill}</span>
                `}
              </div>
            </button>
          `;
        }).join("")}
      </div>
    </div>
  `;
}

export function attachLangModalEvents() {
  const backdrop = document.getElementById("langModalBackdrop");
  const closeBtn = document.getElementById("closeLangModalBtn");

  backdrop?.addEventListener("click", () => store.closeLangModal());
  closeBtn?.addEventListener("click", () => store.closeLangModal());

  document.querySelectorAll(".rg-lang-card").forEach(card => {
    card.addEventListener("click", () => {
      const code = card.getAttribute("data-lang-code");
      if (code) {
        store.setLanguage(code);
        store.closeLangModal();
      }
    });
  });
}
