/**
 * RailGaadi — Route Search (Station A → Station B)
 * Finds all trains running between two Indian stations
 * with day-of-week running routine calendar like "Where is My Train" app
 */

import { store } from "../state/store.js";
import { trackerService } from "../services/trackerService.js";
import { searchTrainsBetweenStations } from "../services/railradarService.js";
import { stationDataService } from "../services/stationDataService.js";

// ── Module state ──────────────────────────────────────────────────────────

let fromStation  = null;  // { code, name }
let toStation    = null;  // { code, name }
let routeResults = [];
let isSearching  = false;
let fromQuery    = "";
let toQuery      = "";
let fromDropOpen = false;
let toDropOpen   = false;
let fromSuggestions = [];
let toSuggestions   = [];
let fromDebounce = null;
let toDebounce   = null;
let dayFilter    = "all"; // "all" | "today" | "tomorrow" | "0"..."6"

// ── Day constants ─────────────────────────────────────────────────────────

const DAY_LABELS = ["S", "M", "T", "W", "T", "F", "S"];
const DAY_NAMES  = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
const TODAY_IDX  = new Date().getDay(); // 0=Sun, 1=Mon … 6=Sat
const TOMORROW_IDX = (TODAY_IDX + 1) % 7;

// ── Popular Sample Routes ─────────────────────────────────────────────────

const POPULAR_ROUTES = [
  { fromCode: "JP",   fromName: "Jaipur Jn",          toCode: "MTJ",  toName: "Mathura Jn" },
  { fromCode: "NDLS", fromName: "New Delhi",          toCode: "CNB",  toName: "Kanpur Central" },
  { fromCode: "BME",  fromName: "Barmer",             toCode: "JU",   toName: "Jodhpur Jn" },
  { fromCode: "HWH",  fromName: "Howrah Jn",          toCode: "PNBE", toName: "Patna Jn" },
  { fromCode: "CSMT", fromName: "Mumbai CSMT",        toCode: "PUNE", toName: "Pune Jn" },
  { fromCode: "MAS",  fromName: "Chennai Central",    toCode: "SBC",  toName: "KSR Bengaluru" },
];

// ── Helpers ───────────────────────────────────────────────────────────────

/** Parse a days-of-week bitmask/array/string into a 7-element boolean array */
function parseDays(raw) {
  if (!raw) return Array(7).fill(true); // assume all days if unknown

  // If it's a 7-char string like "YNNYYNY"
  if (typeof raw === "string" && raw.length === 7) {
    return raw.split("").map(c => c === "Y" || c === "1" || c === "y" || c === "T" || c === "t");
  }

  // If it's a bitmask number (bit 0 = Sun)
  if (typeof raw === "number") {
    return DAY_LABELS.map((_, i) => !!(raw & (1 << i)));
  }

  // If it's an array of day strings/numbers (e.g. ['mon', 'wed', 'thu', 'sat'])
  if (Array.isArray(raw)) {
    const lc = raw.map(d => String(d).toLowerCase().trim());
    const matches = [
      ["sun", "sunday",    "0"],
      ["mon", "monday",    "1"],
      ["tue", "tuesday",   "2"],
      ["wed", "wednesday", "3"],
      ["thu", "thursday",  "4"],
      ["fri", "friday",    "5"],
      ["sat", "saturday",  "6"],
    ];
    return matches.map(alts => alts.some(a => lc.includes(a)));
  }

  // If it's an object with boolean properties
  if (typeof raw === "object") {
    const map = raw;
    return [
      !!(map.sun || map.sunday    || map[0]),
      !!(map.mon || map.monday    || map[1]),
      !!(map.tue || map.tuesday   || map[2]),
      !!(map.wed || map.wednesday || map[3]),
      !!(map.thu || map.thursday  || map[4]),
      !!(map.fri || map.friday    || map[5]),
      !!(map.sat || map.saturday  || map[6]),
    ];
  }

  return Array(7).fill(true);
}

/** Render the S M T W T F S week calendar row */
function renderDaysBadges(days) {
  return `
    <div class="rs-days-row" aria-label="Day-wise running schedule">
      ${DAY_LABELS.map((label, i) => `
        <span class="rs-day-pill ${days[i] ? "rs-day-on" : "rs-day-off"} ${i === TODAY_IDX && days[i] ? "rs-day-today" : ""}"
              title="${DAY_NAMES[i]}${days[i] ? " (runs)" : " (no service)"}"
              aria-label="${DAY_NAMES[i]}${days[i] ? " (runs)" : " (no service)"}">
          ${label}
        </span>
      `).join("")}
    </div>
  `;
}

/** Format duration in minutes as "X hrs Y mins" (just like Where is My Train) */
function fmtDuration(minutes) {
  if (!minutes || isNaN(minutes)) return "";
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h > 0 && m > 0) return `${h} hrs ${m} mins`;
  if (h > 0) return `${h} hrs`;
  return `${m} mins`;
}

/** Format time string "HH:MM" */
function fmtTime(t) {
  if (!t || t === "—") return "—";
  return t.replace(/^(\d):/, "0$1:").slice(0, 5);
}

/** Station autocomplete from stationDataService */
async function getStationSuggestions(query) {
  if (!query || query.length < 2) return [];
  const q = query.toUpperCase().trim();
  const results = [];

  // Search through loaded stations
  stationDataService.stationsMap.forEach((stn, code) => {
    const nameUpper = (stn.name || "").toUpperCase();
    if (
      code.startsWith(q) ||
      nameUpper.includes(q) ||
      nameUpper.startsWith(q)
    ) {
      results.push({ code, name: stn.name || code });
    }
  });

  results.sort((a, b) => {
    const aCode = a.code === q ? 0 : 1;
    const bCode = b.code === q ? 0 : 1;
    if (aCode !== bCode) return aCode - bCode;
    const aStart = a.name.toUpperCase().startsWith(q) ? 0 : 1;
    const bStart = b.name.toUpperCase().startsWith(q) ? 0 : 1;
    if (aStart !== bStart) return aStart - bStart;
    return a.name.localeCompare(b.name);
  });

  return results.slice(0, 8);
}

// ── Render ────────────────────────────────────────────────────────────────

export function renderRouteSearch() {
  return `
    <section class="rs-section" id="routeSearchSection" aria-label="Search trains by route">

      <!-- Header Banner (Where is My Train style) -->
      <div class="rs-hero-card">
        <div class="rs-hero-title">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <rect width="16" height="16" x="4" y="3" rx="2"/><path d="M4 11h16"/><path d="M12 3v8"/><path d="m8 19-2 3"/><path d="m18 22-2-3"/>
          </svg>
          <h2>Find Trains Between Stations</h2>
        </div>
        <p class="rs-hero-subtitle">Check day-wise weekly running routine, timings & live status across India</p>

        <!-- From / To Card -->
        <div class="rs-card">
          <div class="rs-fields">

            <!-- From field -->
            <div class="rs-field-row rs-from-row">
              <div class="rs-field-icon rs-from-icon" aria-hidden="true">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="7"/>
                </svg>
              </div>
              <div class="rs-field-input-wrap" id="rsFromWrap">
                <div class="rs-field-top">
                  <label class="rs-field-label" for="rsFromInput">From Station</label>
                  ${fromStation ? `<span class="rs-station-code-badge">${fromStation.code}</span>` : ""}
                </div>
                <input
                  id="rsFromInput"
                  class="rs-field-input"
                  type="text"
                  inputmode="text"
                  autocomplete="off"
                  spellcheck="false"
                  placeholder="Enter source station (e.g. JP, Jaipur)"
                  value="${fromStation ? fromStation.name : fromQuery}"
                  aria-label="Departure station"
                />
                ${fromStation ? `
                  <button class="rs-field-clear" id="rsClearFrom" aria-label="Clear departure station" title="Clear">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M18 6 6 18"/><path d="m6 6 12 12"/>
                    </svg>
                  </button>
                ` : ""}
              </div>
            </div>

            <!-- Connector line + swap button -->
            <div class="rs-connector-row">
              <div class="rs-connector-line" aria-hidden="true"></div>
              <button class="rs-swap-btn" id="rsSwapBtn" aria-label="Swap from and to stations" title="Swap stations">
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M7 16V4m0 0L3 8m4-4 4 4"/><path d="M17 8v12m0 0 4-4m-4 4-4-4"/>
                </svg>
              </button>
            </div>

            <!-- To field -->
            <div class="rs-field-row rs-to-row">
              <div class="rs-field-icon rs-to-icon" aria-hidden="true">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/>
                  <circle cx="12" cy="10" r="3"/>
                </svg>
              </div>
              <div class="rs-field-input-wrap" id="rsToWrap">
                <div class="rs-field-top">
                  <label class="rs-field-label" for="rsToInput">To Station</label>
                  ${toStation ? `<span class="rs-station-code-badge">${toStation.code}</span>` : ""}
                </div>
                <input
                  id="rsToInput"
                  class="rs-field-input"
                  type="text"
                  inputmode="text"
                  autocomplete="off"
                  spellcheck="false"
                  placeholder="Enter destination station (e.g. MTJ, Mathura)"
                  value="${toStation ? toStation.name : toQuery}"
                  aria-label="Destination station"
                />
                ${toStation ? `
                  <button class="rs-field-clear" id="rsClearTo" aria-label="Clear destination station" title="Clear">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M18 6 6 18"/><path d="m6 6 12 12"/>
                    </svg>
                  </button>
                ` : ""}
              </div>
            </div>
          </div>

          <!-- Find Trains button (matches Where is My Train green button) -->
          <button
            class="rs-find-btn ${isSearching ? "rs-find-btn--loading" : ""}"
            id="rsFindBtn"
            ${isSearching ? "disabled" : ""}
            aria-busy="${isSearching}"
          >
            ${isSearching
              ? `<span class="rs-spinner" aria-hidden="true"></span> Searching all trains…`
              : `
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
                </svg>
                Find trains
              `
            }
          </button>
        </div>

        <!-- Autocomplete dropdowns -->
        ${fromDropOpen && fromSuggestions.length > 0 ? `
          <div class="rs-dropdown" id="rsFromDropdown" role="listbox" aria-label="Departure station suggestions">
            ${fromSuggestions.map(s => `
              <div class="rs-dropdown-item" data-code="${s.code}" data-name="${s.name}" data-field="from" role="option">
                <span class="rs-dropdown-code">${s.code}</span>
                <span class="rs-dropdown-name">${s.name}</span>
              </div>
            `).join("")}
          </div>
        ` : ""}

        ${toDropOpen && toSuggestions.length > 0 ? `
          <div class="rs-dropdown rs-dropdown-to" id="rsToDropdown" role="listbox" aria-label="Destination station suggestions">
            ${toSuggestions.map(s => `
              <div class="rs-dropdown-item" data-code="${s.code}" data-name="${s.name}" data-field="to" role="option">
                <span class="rs-dropdown-code">${s.code}</span>
                <span class="rs-dropdown-name">${s.name}</span>
              </div>
            `).join("")}
          </div>
        ` : ""}

        <!-- Popular Routes Quick Access -->
        <div class="rs-popular-section">
          <div class="rs-popular-label">Popular Indian Routes:</div>
          <div class="rs-popular-chips">
            ${POPULAR_ROUTES.map(r => `
              <button class="rs-popular-chip" data-from-code="${r.fromCode}" data-from-name="${r.fromName}" data-to-code="${r.toCode}" data-to-name="${r.toName}">
                <span>${r.fromCode}</span>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
                <span>${r.toCode}</span>
                <small>(${r.fromName.split(" ")[0]} - ${r.toName.split(" ")[0]})</small>
              </button>
            `).join("")}
          </div>
        </div>
      </div>

      <!-- Results section -->
      ${renderRouteResults()}

    </section>
  `;
}

function renderRouteResults() {
  if (!fromStation || !toStation) return "";

  if (isSearching) {
    return `
      <div class="rs-results-loading" aria-live="polite" aria-busy="true">
        <div class="rs-skeleton-card"></div>
        <div class="rs-skeleton-card"></div>
        <div class="rs-skeleton-card"></div>
      </div>
    `;
  }

  if (routeResults.length === 0) {
    return `
      <div class="rs-empty" role="status">
        <div class="rs-empty-icon" aria-hidden="true">🚂</div>
        <p class="rs-empty-title">No direct trains found</p>
        <p class="rs-empty-sub">No trains currently listed directly between <strong>${fromStation.name} (${fromStation.code})</strong> and <strong>${toStation.name} (${toStation.code})</strong>.<br>Try nearby major junction stations.</p>
      </div>
    `;
  }

  // Apply day filter
  let filtered = routeResults;
  let targetDayIdx = null;

  if (dayFilter === "today") targetDayIdx = TODAY_IDX;
  else if (dayFilter === "tomorrow") targetDayIdx = TOMORROW_IDX;
  else if (dayFilter !== "all") targetDayIdx = parseInt(dayFilter, 10);

  if (targetDayIdx !== null) {
    filtered = routeResults.filter(t => {
      const days = parseDays(t.runningDays || t.days);
      return days[targetDayIdx];
    });
  }

  return `
    <div class="rs-results" role="list" aria-label="Trains from ${fromStation.name} to ${toStation.name}">

      <!-- Filter bar (matches Where is My Train top bar) -->
      <div class="rs-filter-bar">
        <div class="rs-filter-left">
          <label for="rsDayFilter" class="rs-filter-label">Filter by:</label>
          <div class="rs-filter-select-wrap">
            <select id="rsDayFilter" class="rs-filter-select" aria-label="Filter trains by day">
              <option value="all" ${dayFilter === "all" ? "selected" : ""}>All Dates</option>
              <option value="today" ${dayFilter === "today" ? "selected" : ""}>Today (${DAY_NAMES[TODAY_IDX]})</option>
              <option value="tomorrow" ${dayFilter === "tomorrow" ? "selected" : ""}>Tomorrow (${DAY_NAMES[TOMORROW_IDX]})</option>
              ${DAY_NAMES.map((name, idx) => `
                <option value="${idx}" ${dayFilter === String(idx) ? "selected" : ""}>Every ${name}</option>
              `).join("")}
            </select>
            <svg class="rs-select-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m6 9 6 6 6-6"/></svg>
          </div>
        </div>

        <div class="rs-filter-count">
          Showing <strong>${filtered.length}</strong> of ${routeResults.length} trains
        </div>
      </div>

      <!-- Route Header card -->
      <div class="rs-route-summary-card">
        <div class="rs-summary-stn">
          <div class="rs-summary-code">${fromStation.code}</div>
          <div class="rs-summary-name">${fromStation.name}</div>
        </div>
        <div class="rs-summary-divider">
          <div class="rs-summary-line"></div>
          <div class="rs-summary-dot"></div>
        </div>
        <div class="rs-summary-stn rs-summary-stn--right">
          <div class="rs-summary-code">${toStation.code}</div>
          <div class="rs-summary-name">${toStation.name}</div>
        </div>
      </div>

      <!-- Trains list -->
      ${filtered.length === 0 ? `
        <div class="rs-no-filter-match">
          <p>No trains running on selected day.</p>
          <button class="rs-reset-filter-btn" id="rsResetFilter">Show all ${routeResults.length} trains</button>
        </div>
      ` : filtered.map(t => {
        const days = parseDays(t.runningDays || t.days);
        const runsToday = days[TODAY_IDX];
        const depTime   = fmtTime(t.departureTime || t.dep || t.departure);
        const arrTime   = fmtTime(t.arrivalTime   || t.arr || t.arrival);
        const duration  = t.durationMinutes || t.duration;

        return `
          <div class="rs-result-card ${runsToday ? "" : "rs-result-card--not-today"}"
               data-train-id="${t.trainId || t.number}"
               role="listitem">

            <!-- Train number & title -->
            <div class="rs-result-top">
              <div class="rs-result-identity">
                <span class="rs-result-num">${t.number}</span>
                <span class="rs-result-name">${t.name}</span>
              </div>
              ${runsToday
                ? `<span class="rs-runs-badge rs-runs-badge--today">Runs today</span>`
                : `<span class="rs-runs-badge rs-runs-badge--not-today">Not running today</span>`
              }
            </div>

            <!-- Timings row -->
            <div class="rs-result-timing">
              <div class="rs-time-block">
                <span class="rs-time">${depTime}</span>
                <span class="rs-time-stn-label">${fromStation.code}</span>
              </div>

              <div class="rs-duration-line">
                ${duration ? `<span class="rs-duration-label">${fmtDuration(duration)}</span>` : ""}
                <div class="rs-duration-track">
                  <div class="rs-duration-dot rs-duration-dot--start"></div>
                  <div class="rs-duration-bar"></div>
                  <div class="rs-duration-dot rs-duration-dot--end"></div>
                </div>
              </div>

              <div class="rs-time-block rs-time-block--right">
                <span class="rs-time">${arrTime}</span>
                <span class="rs-time-stn-label">${toStation.code}</span>
              </div>
            </div>

            <!-- Week Calendar Routine (S M T W T F S) -->
            <div class="rs-calendar-wrap">
              <div class="rs-calendar-label">Weekly Schedule:</div>
              ${renderDaysBadges(days)}
            </div>

            <!-- Action footer -->
            <div class="rs-result-footer">
              ${t.type ? `<span class="rs-train-type-pill">${t.type}</span>` : ""}
              ${t.platform && t.platform !== "—" ? `<span class="rs-platform-tag">Plt ${t.platform}</span>` : ""}
              <button class="rs-track-btn" data-train-id="${t.trainId || t.number}" aria-label="Track live train ${t.number}">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <polygon points="5 3 19 12 5 21 5 3"/>
                </svg>
                Track Live
              </button>
            </div>

          </div>
        `;
      }).join("")}
    </div>
  `;
}

// ── Events ────────────────────────────────────────────────────────────────

function rerenderSection() {
  const sec = document.getElementById("routeSearchSection");
  if (!sec) return;
  sec.outerHTML = renderRouteSearch();
  attachRouteSearchEvents();
}

async function doSearch() {
  if (!fromStation || !toStation) {
    const inp = !fromStation ? document.getElementById("rsFromInput") : document.getElementById("rsToInput");
    inp?.focus();
    inp?.classList.add("rs-input-shake");
    setTimeout(() => inp?.classList.remove("rs-input-shake"), 400);
    return;
  }

  isSearching  = true;
  routeResults = [];
  rerenderSection();

  try {
    const results = await searchTrainsBetweenStations(fromStation.code, toStation.code);
    routeResults  = results || [];
  } catch (err) {
    console.warn("[RouteSearch] search failed:", err.message);
    routeResults = [];
  } finally {
    isSearching = false;
    rerenderSection();
  }
}

export function attachRouteSearchEvents() {
  // Swap button
  document.getElementById("rsSwapBtn")?.addEventListener("click", () => {
    [fromStation, toStation] = [toStation, fromStation];
    [fromQuery, toQuery]     = [toQuery, fromQuery];
    fromDropOpen = false;
    toDropOpen   = false;
    rerenderSection();
    if (fromStation && toStation) doSearch();
  });

  // Clear buttons
  document.getElementById("rsClearFrom")?.addEventListener("click", () => {
    fromStation = null; fromQuery = ""; fromDropOpen = false;
    rerenderSection();
    document.getElementById("rsFromInput")?.focus();
  });

  document.getElementById("rsClearTo")?.addEventListener("click", () => {
    toStation = null; toQuery = ""; toDropOpen = false;
    rerenderSection();
    document.getElementById("rsToInput")?.focus();
  });

  // Find trains button
  document.getElementById("rsFindBtn")?.addEventListener("click", doSearch);

  // Day filter select
  document.getElementById("rsDayFilter")?.addEventListener("change", e => {
    dayFilter = e.target.value;
    rerenderSection();
  });

  document.getElementById("rsResetFilter")?.addEventListener("click", () => {
    dayFilter = "all";
    rerenderSection();
  });

  // Popular route chips click
  document.querySelectorAll(".rs-popular-chip").forEach(chip => {
    chip.addEventListener("click", () => {
      const fromCode = chip.getAttribute("data-from-code");
      const fromName = chip.getAttribute("data-from-name");
      const toCode   = chip.getAttribute("data-to-code");
      const toName   = chip.getAttribute("data-to-name");
      fromStation = { code: fromCode, name: fromName };
      toStation   = { code: toCode,   name: toName };
      fromQuery   = fromName;
      toQuery     = toName;
      doSearch();
    });
  });

  // From input
  const fromInput = document.getElementById("rsFromInput");
  fromInput?.addEventListener("input", e => {
    fromQuery = e.target.value;
    fromStation = null;
    clearTimeout(fromDebounce);
    fromDebounce = setTimeout(async () => {
      fromSuggestions = await getStationSuggestions(fromQuery);
      fromDropOpen    = fromSuggestions.length > 0;
      rerenderSection();
      document.getElementById("rsFromInput")?.focus();
    }, 200);
  });

  fromInput?.addEventListener("keydown", e => {
    if (e.key === "Enter") {
      if (fromSuggestions.length > 0) {
        const s = fromSuggestions[0];
        fromStation  = s;
        fromQuery    = s.name;
        fromDropOpen = false;
        rerenderSection();
        document.getElementById("rsToInput")?.focus();
      } else {
        document.getElementById("rsToInput")?.focus();
      }
    }
    if (e.key === "Escape") { fromDropOpen = false; rerenderSection(); }
  });

  // To input
  const toInput = document.getElementById("rsToInput");
  toInput?.addEventListener("input", e => {
    toQuery = e.target.value;
    toStation = null;
    clearTimeout(toDebounce);
    toDebounce = setTimeout(async () => {
      toSuggestions = await getStationSuggestions(toQuery);
      toDropOpen    = toSuggestions.length > 0;
      rerenderSection();
      document.getElementById("rsToInput")?.focus();
    }, 200);
  });

  toInput?.addEventListener("keydown", e => {
    if (e.key === "Enter") {
      if (toSuggestions.length > 0) {
        const s = toSuggestions[0];
        toStation  = s;
        toQuery    = s.name;
        toDropOpen = false;
        rerenderSection();
        document.getElementById("rsFindBtn")?.focus();
      } else {
        doSearch();
      }
    }
    if (e.key === "Escape") { toDropOpen = false; rerenderSection(); }
  });

  // Dropdown item click
  document.querySelectorAll(".rs-dropdown-item").forEach(item => {
    item.addEventListener("click", () => {
      const code  = item.getAttribute("data-code");
      const name  = item.getAttribute("data-name");
      const field = item.getAttribute("data-field");
      if (field === "from") {
        fromStation  = { code, name };
        fromQuery    = name;
        fromDropOpen = false;
        rerenderSection();
        document.getElementById("rsToInput")?.focus();
      } else {
        toStation  = { code, name };
        toQuery    = name;
        toDropOpen = false;
        rerenderSection();
        doSearch();
      }
    });
  });

  // Track buttons in results
  document.querySelectorAll(".rs-track-btn").forEach(btn => {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      const trainId = btn.getAttribute("data-train-id");
      if (trainId) {
        trackerService.loadTrain(trainId);
        store.setActiveTab("tracking");
      }
    });
  });

  // Whole card click also tracks
  document.querySelectorAll(".rs-result-card").forEach(card => {
    card.addEventListener("click", (e) => {
      if (e.target.closest(".rs-track-btn") || e.target.closest("select")) return;
      const trainId = card.getAttribute("data-train-id");
      if (trainId) {
        trackerService.loadTrain(trainId);
        store.setActiveTab("tracking");
      }
    });
  });

  // Close dropdowns on outside click
  document.addEventListener("click", e => {
    const insideFrom = document.getElementById("rsFromWrap")?.contains(e.target);
    const insideTo   = document.getElementById("rsToWrap")?.contains(e.target);
    const insideDropFrom = document.getElementById("rsFromDropdown")?.contains(e.target);
    const insideDropTo   = document.getElementById("rsToDropdown")?.contains(e.target);
    if (!insideFrom && !insideDropFrom && fromDropOpen) {
      fromDropOpen = false;
      rerenderSection();
    }
    if (!insideTo && !insideDropTo && toDropOpen) {
      toDropOpen = false;
      rerenderSection();
    }
  }, { capture: true, once: true });
}

export { renderDaysBadges, parseDays };
