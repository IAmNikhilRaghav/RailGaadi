/**
 * RailGaadi Favourites Drawer — Phase 2
 * Slide-out panel showing saved and recent trains with one-click load
 */

import { store } from "../state/store.js";
import { trackerService } from "../services/trackerService.js";

export function renderFavDrawer() {
  const state = store.getState();
  if (!state.isFavDrawerOpen) return "";

  const { favourites, recentSearches, activeTrainId } = state;

  function getTrainInfo(id) {
    const meta = store.getTrainMeta(id);
    return {
      id,
      number: meta?.number || id,
      name:   meta?.name   || `Train #${id}`,
      origin: meta?.origin || "",
      dest:   meta?.destination || "",
      type:   meta?.type   || "Express"
    };
  }

  const favTrains    = favourites.map(getTrainInfo);
  const recentTrains = recentSearches
    .filter(id => !favourites.includes(id))
    .slice(0, 6)
    .map(getTrainInfo);

  function trainRow(t, isFav) {
    const isActive = t.id === activeTrainId;
    return `
      <div class="fav-train-row ${isActive ? "active" : ""}" data-train-id="${t.id}">
        <div class="fav-train-info">
          <div class="fav-train-number-badge">${t.number}</div>
          <div class="fav-train-meta">
            <div class="fav-train-name">${t.name}</div>
            ${t.origin && t.dest ? `
              <div class="fav-train-route">${t.origin} → ${t.dest}</div>
            ` : ""}
          </div>
        </div>
        <div class="fav-train-row-actions">
          <span class="fav-type-tag">${(t.type || "Express").split("/")[0]}</span>
          <button class="fav-unstar-btn" data-train-id="${t.id}" title="${isFav ? "Remove from favourites" : "Add to favourites"}">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="${isFav ? "#f59e0b" : "none"}" stroke="${isFav ? "#f59e0b" : "currentColor"}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
          </button>
        </div>
      </div>
    `;
  }

  return `
    <div class="fav-drawer-backdrop" id="favDrawerBackdrop"></div>
    <aside class="fav-drawer" id="favDrawerPanel" role="complementary" aria-label="Favourites">
      <div class="fav-drawer-header">
        <div style="display:flex;align-items:center;gap:10px;">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="#f59e0b" stroke="#f59e0b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
          </svg>
          <h2 style="font-size:var(--text-lg);font-weight:700;color:var(--color-text-primary);">Saved Trains</h2>
        </div>
        <button class="header-btn btn-icon-only" id="closeFavDrawerBtn" aria-label="Close drawer">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 6 6 18"/><path d="m6 6 12 12"/>
          </svg>
        </button>
      </div>

      <div class="fav-drawer-body">
        <!-- Favourites -->
        <div class="fav-drawer-section">
          <div class="fav-drawer-section-label">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="#f59e0b" stroke="#f59e0b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
            <span>Favourites (${favTrains.length})</span>
          </div>
          ${favTrains.length > 0
            ? favTrains.map(t => trainRow(t, true)).join("")
            : `<div class="fav-drawer-empty">⭐ No favourites yet — star a train from the status card</div>`}
        </div>

        <!-- Recent Searches -->
        ${recentTrains.length > 0 ? `
          <div class="fav-drawer-section">
            <div class="fav-drawer-section-label">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
              </svg>
              <span>Recent (${recentTrains.length})</span>
            </div>
            ${recentTrains.map(t => trainRow(t, false)).join("")}
          </div>
        ` : ""}
      </div>
    </aside>
  `;
}

export function attachFavDrawerEvents() {
  const backdrop = document.getElementById("favDrawerBackdrop");
  const closeBtn = document.getElementById("closeFavDrawerBtn");

  backdrop?.addEventListener("click", () => store.closeFavDrawer());
  closeBtn?.addEventListener("click",  () => store.closeFavDrawer());

  // Click on train row → load train & close drawer
  document.querySelectorAll(".fav-train-row").forEach(row => {
    row.addEventListener("click", (e) => {
      if (e.target.closest(".fav-unstar-btn")) return; // handled separately
      const id = row.getAttribute("data-train-id");
      if (id) {
        trackerService.loadTrain(id);
        store.closeFavDrawer();
      }
    });
  });

  // Star/unstar buttons inside the drawer
  document.querySelectorAll(".fav-unstar-btn").forEach(btn => {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      const id = btn.getAttribute("data-train-id");
      if (id) store.toggleFavourite(id);
    });
  });
}
