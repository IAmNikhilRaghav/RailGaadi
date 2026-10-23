/**
 * RailGaadi Search Hero — v2
 * Async API search with immediate responsiveness, "Track Live" button,
 * keyboard Enter submission, and quick access chips.
 */

import { store } from "../state/store.js";
import { trackerService } from "../services/trackerService.js";

let selectedIndex   = -1;
let debounceTimer   = null;
let abortController = null;

/* ── helpers ──────────────────────────────────────────────── */

function getChipLabel(trainId) {
  const meta = store.getTrainMeta(trainId);
  if (!meta) return { number: trainId, shortName: "" };
  const parts = (meta.name || "").split(" ");
  return { number: meta.number || trainId, shortName: parts.slice(0, 2).join(" ") };
}

/* ── render ──────────────────────────────────────────────── */

export function renderQuickChips() {
  const state = store.getState();
  const { favourites, recentSearches, activeTrainId } = state;

  const allIds = [...new Set([...favourites, ...recentSearches])].slice(0, 8);
  const chips = allIds.map(id => ({
    id,
    isFav: favourites.includes(id),
    ...getChipLabel(id)
  }));

  return `
    <span style="font-size:11px;font-weight:700;color:var(--color-text-muted);text-transform:uppercase;white-space:nowrap;margin-right:4px;">Quick:</span>
    ${chips.length > 0 ? chips.map(c => `
      <button class="quick-chip ${c.id === activeTrainId ? "active" : ""}" data-train-id="${c.id}" title="Load ${c.number}">
        ${c.isFav ? "⭐" : "🕒"} <strong>${c.number}</strong>${c.shortName ? ` · ${c.shortName}` : ""}
      </button>
    `).join("") : `
      <button class="quick-chip ${activeTrainId === "22436" ? "active" : ""}" data-train-id="22436">⭐ <strong>22436</strong> · Vande Bharat</button>
      <button class="quick-chip ${activeTrainId === "12301" ? "active" : ""}" data-train-id="12301">⭐ <strong>12301</strong> · Howrah Rajdhani</button>
      <button class="quick-chip ${activeTrainId === "12952" ? "active" : ""}" data-train-id="12952">⭐ <strong>12952</strong> · Tejas Rajdhani</button>
      <button class="quick-chip ${activeTrainId === "12002" ? "active" : ""}" data-train-id="12002">⭐ <strong>12002</strong> · Shatabdi</button>
    `}
  `;
}

export function updateQuickChips() {
  const bar = document.getElementById("quickAccessBar");
  if (!bar) return;
  bar.innerHTML = renderQuickChips();
  attachQuickChipEvents();
}

function attachQuickChipEvents() {
  document.querySelectorAll(".quick-chip").forEach(chip => {
    chip.addEventListener("click", () => {
      const trainId = chip.getAttribute("data-train-id");
      if (trainId) {
        trackerService.loadTrain(trainId);
      }
    });
  });
}

export function renderSearchSection() {
  return `
    <section class="search-hero-section" aria-label="Train Search">
      <div class="search-container">
        <div class="search-input-wrapper" id="searchInputWrapper">
          <div class="search-icon-left" id="searchIconBtn" title="Search">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
            </svg>
          </div>

          <input
            type="text"
            class="search-input"
            id="trainSearchInput"
            placeholder="Search any Indian train by number or name (e.g. 12301, Rajdhani, Vande Bharat)…"
            autocomplete="off"
            spellcheck="false"
            aria-autocomplete="list"
            aria-expanded="false"
            aria-controls="searchResultsDropdown"
          />

          <!-- Loading spinner inside input -->
          <div id="searchSpinner" style="display:none; color:var(--color-accent); margin-right:8px; align-items:center;">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" style="animation:spin 0.8s linear infinite;">
              <path d="M21 12a9 9 0 1 1-6.22-8.56"/>
            </svg>
          </div>

          <!-- Clear button (toggled via style.display) -->
          <button class="search-clear-btn" id="searchClearBtn" title="Clear" aria-label="Clear search" style="display:none;">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6 6 18"/><path d="m6 6 12 12"/>
            </svg>
          </button>

          <!-- Track Live Action Button -->
          <button class="search-submit-btn" id="searchSubmitBtn" title="Track Train">
            <span>Track Live</span>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M5 12h14"/><path d="m12 5 7 7-7 7"/>
            </svg>
          </button>
        </div>

        <!-- Results dropdown -->
        <div class="search-dropdown" id="searchResultsDropdown" style="display:none;" role="listbox" aria-label="Search results"></div>
      </div>

      <!-- Quick Access Chips -->
      <div class="quick-access-bar" id="quickAccessBar" role="toolbar" aria-label="Quick access trains">
        ${renderQuickChips()}
      </div>

      <style>
        @keyframes spin { to { transform: rotate(360deg); } }
      </style>
    </section>
  `;
}


/* ── events ──────────────────────────────────────────────── */

export function attachSearchEvents() {
  const input     = document.getElementById("trainSearchInput");
  const dropdown  = document.getElementById("searchResultsDropdown");
  const clearBtn  = document.getElementById("searchClearBtn");
  const spinner   = document.getElementById("searchSpinner");
  const submitBtn = document.getElementById("searchSubmitBtn");

  if (!input || !dropdown) return;

  function closeDropdown() {
    dropdown.style.display = "none";
    input.setAttribute("aria-expanded", "false");
    selectedIndex = -1;
  }

  function selectTrain(item, results) {
    const trainId = item.getAttribute("data-train-id");
    const meta    = results?.find(r => r.trainId === trainId);
    closeDropdown();
    input.value = "";
    if (clearBtn) clearBtn.style.display = "none";
    if (spinner)  spinner.style.display = "none";
    trackerService.loadTrain(trainId, meta);
  }

  async function submitSearch(query) {
    const val = (query || "").trim();
    if (!val) {
      input.focus();
      return;
    }

    closeDropdown();
    if (spinner) spinner.style.display = "flex";

    try {
      const results = await trackerService.searchTrains(val);
      if (spinner) spinner.style.display = "none";
      if (results && results.length > 0) {
        const top = results[0];
        input.value = "";
        if (clearBtn) clearBtn.style.display = "none";
        trackerService.loadTrain(top.trainId, top);
      } else {
        input.value = "";
        if (clearBtn) clearBtn.style.display = "none";
        trackerService.loadTrain(val);
      }
    } catch {
      if (spinner) spinner.style.display = "none";
      input.value = "";
      if (clearBtn) clearBtn.style.display = "none";
      trackerService.loadTrain(val);
    }
  }

  function showResults(results, query) {
    if (!results || results.length === 0) {
      dropdown.innerHTML = `
        <div style="padding:20px;text-align:center;color:var(--color-text-muted);">
          <div style="font-size:24px;margin-bottom:8px;">🔍</div>
          <div style="font-weight:600;color:var(--color-text-primary);margin-bottom:4px;">No trains found for "${query}"</div>
          <div style="font-size:12px;">Try entering a 5-digit train number (e.g. 22436, 12301) or train name (e.g. Vande Bharat, Rajdhani)</div>
        </div>
      `;
      dropdown.style.display = "block";
      input.setAttribute("aria-expanded", "true");
      return;
    }

    selectedIndex = -1;
    dropdown.innerHTML = results.map((t, idx) => `
      <div class="search-result-item" data-index="${idx}" data-train-id="${t.trainId}" role="option" tabindex="-1">
        <div class="result-main">
          <div class="result-train-title">
            <span class="train-number-badge">${t.number}</span>
            <span class="train-name-text">${t.name}</span>
          </div>
          <div class="result-route-sub">
            <span>${t.origin || "Origin"}</span>
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
            <span>${t.destination || "Destination"}</span>
          </div>
        </div>
        <span class="result-type-tag">${(t.type || "Express").split("/")[0].trim()}</span>
      </div>
    `).join("");

    dropdown.style.display = "block";
    input.setAttribute("aria-expanded", "true");

    // Bind item clicks
    dropdown.querySelectorAll(".search-result-item").forEach(item => {
      item.addEventListener("click", () => selectTrain(item, results));
    });
  }

  /* ── debounced input ── */
  input.addEventListener("input", e => {
    const val = e.target.value;

    if (clearBtn) {
      clearBtn.style.display = val.length > 0 ? "flex" : "none";
    }

    clearTimeout(debounceTimer);
    if (abortController) {
      abortController.abort();
      abortController = null;
    }

    if (!val.trim() || val.trim().length < 2) {
      closeDropdown();
      if (spinner) spinner.style.display = "none";
      return;
    }

    if (spinner) spinner.style.display = "flex";

    debounceTimer = setTimeout(async () => {
      abortController = new AbortController();
      try {
        const results = await trackerService.searchTrains(val.trim());
        if (spinner) spinner.style.display = "none";
        showResults(results, val.trim());
      } catch (err) {
        if (spinner) spinner.style.display = "none";
      }
    }, 250);
  });

  /* ── keyboard navigation & Enter submission ── */
  input.addEventListener("keydown", e => {
    const items = dropdown.querySelectorAll(".search-result-item");
    const isDropdownVisible = items.length > 0 && dropdown.style.display !== "none";

    if (e.key === "ArrowDown") {
      if (!isDropdownVisible) return;
      e.preventDefault();
      selectedIndex = (selectedIndex + 1) % items.length;
      highlight(items);
    } else if (e.key === "ArrowUp") {
      if (!isDropdownVisible) return;
      e.preventDefault();
      selectedIndex = (selectedIndex - 1 + items.length) % items.length;
      highlight(items);
    } else if (e.key === "Enter") {
      e.preventDefault();
      if (isDropdownVisible && selectedIndex >= 0) {
        items[selectedIndex].click();
      } else if (isDropdownVisible && items.length > 0) {
        items[0].click();
      } else {
        submitSearch(input.value);
      }
    } else if (e.key === "Escape") {
      closeDropdown();
    }
  });

  function highlight(items) {
    items.forEach((it, i) => {
      it.classList.toggle("selected", i === selectedIndex);
      if (i === selectedIndex) it.scrollIntoView({ block: "nearest" });
    });
  }

  /* ── submit button click ── */
  if (submitBtn) {
    submitBtn.addEventListener("click", () => {
      const items = dropdown.querySelectorAll(".search-result-item");
      const isDropdownVisible = items.length > 0 && dropdown.style.display !== "none";
      if (isDropdownVisible && selectedIndex >= 0) {
        items[selectedIndex].click();
      } else if (isDropdownVisible && items.length > 0) {
        items[0].click();
      } else {
        submitSearch(input.value);
      }
    });
  }

  /* ── close on outside click (registered once) ── */
  if (!window._searchDocClickRegistered) {
    window._searchDocClickRegistered = true;
    document.addEventListener("click", e => {
      const currentDropdown = document.getElementById("searchResultsDropdown");
      const wrapper = document.getElementById("searchInputWrapper");
      if (currentDropdown && wrapper && !wrapper.contains(e.target) && !currentDropdown.contains(e.target)) {
        currentDropdown.style.display = "none";
        const currentInput = document.getElementById("trainSearchInput");
        if (currentInput) currentInput.setAttribute("aria-expanded", "false");
        selectedIndex = -1;
      }
    });
  }

  /* ── clear button ── */
  if (clearBtn) {
    clearBtn.addEventListener("click", () => {
      input.value = "";
      clearBtn.style.display = "none";
      if (spinner) spinner.style.display = "none";
      closeDropdown();
      input.focus();
    });
  }

  /* ── quick chip clicks ── */
  attachQuickChipEvents();
}

