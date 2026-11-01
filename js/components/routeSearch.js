/**
 * RailGaadi — Route Search (Station A → Station B)
 * Dedicated full-page route search with instant dynamic train matching
 * across all Indian trains (5,200+ Pan-India trains & 8,690+ stations)
 * Matches any alphabet, word, number, or string with zero delay.
 */

import { store } from "../state/store.js";
import { trackerService } from "../services/trackerService.js";
import { searchTrainsBetweenStations } from "../services/railradarService.js";
import { stationDataService } from "../services/stationDataService.js";

// ── Module state ──────────────────────────────────────────────────────────

let fromStation = null;  // { code, name }
let toStation = null;    // { code, name }
let routeResults = [];
let isSearching = false;
let fromQuery = "";
let toQuery = "";
let fromDropOpen = false;
let toDropOpen = false;
let fromSuggestions = [];
let toSuggestions = [];
let fromDebounce = null;
let toDebounce = null;
let dayFilter = "all";   // "all" | "today" | "tomorrow" | "0"..."6"
let liveFilterQuery = "";
let catalogTrainsCache = [];

// ── Day constants ─────────────────────────────────────────────────────────

const DAY_LABELS = ["S", "M", "T", "W", "T", "F", "S"];
const DAY_NAMES = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
const TODAY_IDX = new Date().getDay(); // 0=Sun, 1=Mon … 6=Sat
const TOMORROW_IDX = (TODAY_IDX + 1) % 7;

// Preload catalog trains
stationDataService.getAllTrains().then(trains => {
  if (trains && trains.length > 0) {
    catalogTrainsCache = trains;
    const container = document.getElementById("rsResultsContainer");
    if (container && routeResults.length === 0) {
      container.innerHTML = renderRouteResultsContent();
      attachResultItemEvents();
    }
  }
});

// ── Helpers ───────────────────────────────────────────────────────────────

/** Parse a days-of-week bitmask/array/string into a 7-element boolean array */
function parseDays(raw) {
  if (!raw) return Array(7).fill(true);

  if (typeof raw === "string" && raw.length === 7) {
    return raw.split("").map(c => c === "Y" || c === "1" || c === "y" || c === "T" || c === "t");
  }

  if (typeof raw === "number") {
    return DAY_LABELS.map((_, i) => !!(raw & (1 << i)));
  }

  if (Array.isArray(raw)) {
    const lc = raw.map(d => String(d).toLowerCase().trim());
    const matches = [
      ["sun", "sunday", "0"],
      ["mon", "monday", "1"],
      ["tue", "tuesday", "2"],
      ["wed", "wednesday", "3"],
      ["thu", "thursday", "4"],
      ["fri", "friday", "5"],
      ["sat", "saturday", "6"],
    ];
    return matches.map(alts => alts.some(a => lc.includes(a)));
  }

  if (typeof raw === "object") {
    const map = raw;
    return [
      !!(map.sun || map.sunday || map[0]),
      !!(map.mon || map.monday || map[1]),
      !!(map.tue || map.tuesday || map[2]),
      !!(map.wed || map.wednesday || map[3]),
      !!(map.thu || map.thursday || map[4]),
      !!(map.fri || map.friday || map[5]),
      !!(map.sat || map.saturday || map[6]),
    ];
  }

  return Array(7).fill(true);
}

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

function fmtDuration(minutes) {
  if (!minutes || isNaN(minutes)) return "";
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h > 0 && m > 0) return `${h} hrs ${m} mins`;
  if (h > 0) return `${h} hrs`;
  return `${m} mins`;
}

function fmtTime(t) {
  if (!t || t === "—") return "—";
  return t.replace(/^(\d):/, "0$1:").slice(0, 5);
}

/** Station autocomplete — searches all 8,690+ stations matching any alphabet or word */
async function getStationSuggestions(query) {
  if (!query || !query.trim()) return [];
  const q = query.toUpperCase().trim();
  const qWords = q.split(/\s+/).filter(Boolean);
  const results = [];
  const seenCodes = new Set();

  await stationDataService.loadAllStations();

  stationDataService.stationsMap.forEach((stn, code) => {
    const nameUpper = (stn.name || "").toUpperCase();
    const matchesCode = code === q || code.startsWith(q) || code.includes(q);
    const matchesName = nameUpper.startsWith(q) || nameUpper.includes(q) || qWords.every(w => nameUpper.includes(w));

    if (matchesCode || matchesName) {
      if (!seenCodes.has(code)) {
        seenCodes.add(code);
        results.push({ code, name: stn.name || code });
      }
    }
  });

  results.sort((a, b) => {
    const aExact = a.code === q ? 0 : 1;
    const bExact = b.code === q ? 0 : 1;
    if (aExact !== bExact) return aExact - bExact;

    const aCodeStart = a.code.startsWith(q) ? 0 : 1;
    const bCodeStart = b.code.startsWith(q) ? 0 : 1;
    if (aCodeStart !== bCodeStart) return aCodeStart - bCodeStart;

    const aStart = a.name.toUpperCase().startsWith(q) ? 0 : 1;
    const bStart = b.name.toUpperCase().startsWith(q) ? 0 : 1;
    if (aStart !== bStart) return aStart - bStart;

    return a.name.localeCompare(b.name);
  });

  return results.slice(0, 15);
}

/** Get trains to display dynamically based on selection and query */
function getTrainsToDisplay() {
  if (routeResults.length > 0) {
    return {
      title: `${fromStation?.name || fromStation?.code} → ${toStation?.name || toStation?.code}`,
      trains: routeResults
    };
  }

  const catalog = catalogTrainsCache;

  // If fromStation is selected
  if (fromStation && !toStation) {
    const fCode = fromStation.code.toUpperCase();
    const fName = fromStation.name.toUpperCase().slice(0, 5);
    const matched = catalog.filter(t => {
      const o = (t.origin || "").toUpperCase();
      const d = (t.destination || "").toUpperCase();
      const n = (t.name || "").toUpperCase();
      return o.includes(fName) || o.includes(fCode) || d.includes(fName) || n.includes(fName);
    });
    return {
      title: `Trains from ${fromStation.name} (${fromStation.code})`,
      trains: matched.slice(0, 40)
    };
  }

  // If toStation is selected
  if (!fromStation && toStation) {
    const tCode = toStation.code.toUpperCase();
    const tName = toStation.name.toUpperCase().slice(0, 5);
    const matched = catalog.filter(t => {
      const o = (t.origin || "").toUpperCase();
      const d = (t.destination || "").toUpperCase();
      const n = (t.name || "").toUpperCase();
      return d.includes(tName) || d.includes(tCode) || o.includes(tName) || n.includes(tName);
    });
    return {
      title: `Trains to ${toStation.name} (${toStation.code})`,
      trains: matched.slice(0, 40)
    };
  }

  // If typing query in station input
  const activeQ = (fromQuery || toQuery || "").trim().toLowerCase();
  if (activeQ.length >= 2) {
    const matched = catalog.filter(t => {
      return (t.number && t.number.toLowerCase().includes(activeQ)) ||
             (t.name && t.name.toLowerCase().includes(activeQ)) ||
             (t.origin && t.origin.toLowerCase().includes(activeQ)) ||
             (t.destination && t.destination.toLowerCase().includes(activeQ)) ||
             (t.type && t.type.toLowerCase().includes(activeQ));
    });
    return {
      title: `Matching Indian Trains for "${activeQ}"`,
      trains: matched.slice(0, 40)
    };
  }

  // Default: prominent Pan-India Trains
  const prominent = [
    { number: "12404", name: "Prayagraj SF Express", origin: "Lalgarh Jn", destination: "Prayagraj Jn", type: "Superfast Express", departureTime: "10:30", arrivalTime: "04:45", durationMinutes: 1095, runningDays: "YYYYYYY", platform: "2" },
    { number: "22436", name: "Vande Bharat Express", origin: "New Delhi", destination: "Varanasi Jn", type: "Vande Bharat", departureTime: "06:00", arrivalTime: "14:00", durationMinutes: 480, runningDays: "YNYYYYY", platform: "1" },
    { number: "12301", name: "Howrah Rajdhani Express", origin: "Howrah Jn", destination: "New Delhi", type: "Rajdhani", departureTime: "16:50", arrivalTime: "10:05", durationMinutes: 1035, runningDays: "YYYYYYN", platform: "9" },
    { number: "12952", name: "Tejas Rajdhani Express", origin: "New Delhi", destination: "Mumbai Central", type: "Tejas Rajdhani", departureTime: "16:55", arrivalTime: "08:35", durationMinutes: 940, runningDays: "YYYYYYY", platform: "3" },
    { number: "12002", name: "Bhopal Shatabdi Express", origin: "New Delhi", destination: "Rani Kamalapati", type: "Shatabdi", departureTime: "06:00", arrivalTime: "14:40", durationMinutes: 520, runningDays: "YYYYYYY", platform: "1" },
    { number: "20490", name: "Barmer SF Express", origin: "Mathura Jn", destination: "Barmer", type: "Superfast Express", departureTime: "15:45", arrivalTime: "06:30", durationMinutes: 885, runningDays: "YYYYYYY", platform: "1" },
    { number: "12259", name: "Sealdah Bikaner AC Duronto", origin: "Sealdah", destination: "Bikaner Jn", type: "Duronto", departureTime: "17:00", arrivalTime: "18:20", durationMinutes: 1520, runningDays: "YYYYYYY", platform: "12" },
    { number: "51974", name: "Jaipur - Mathura Passenger", origin: "Jaipur Jn", destination: "Mathura Jn", type: "Passenger/Local", departureTime: "14:30", arrivalTime: "20:45", durationMinutes: 375, runningDays: "YYYYYYY", platform: "4" }
  ];

  const topCatalog = (catalog || []).slice(0, 25).map(t => ({
    number: t.number,
    name: t.name,
    origin: t.origin,
    destination: t.destination,
    type: t.type || "Express",
    departureTime: "—",
    arrivalTime: "—",
    runningDays: "YYYYYYY"
  }));

  return {
    title: `Pan-India Trains & Routes`,
    trains: [...prominent, ...topCatalog]
  };
}

// ── Render ────────────────────────────────────────────────────────────────

export function renderRouteSearch() {
  return `
    <section class="rs-section" id="routeSearchSection" aria-label="Search trains by route">

      <!-- Route Search Card on Top -->
      <div class="rs-hero-card">
        <div class="rs-hero-title">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <rect width="16" height="16" x="4" y="3" rx="2"/><path d="M4 11h16"/><path d="M12 3v8"/><path d="m8 19-2 3"/><path d="m18 22-2-3"/>
          </svg>
          <h2>Find Trains Between Stations</h2>
        </div>

        <!-- From / To Card -->
        <div class="rs-card">
          <div class="rs-fields">

            <!-- From field -->
            <div class="rs-field-row rs-from-row">
              <div class="rs-field-icon rs-from-icon" aria-hidden="true">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="8"/>
                </svg>
              </div>
              <div class="rs-field-input-wrap" id="rsFromWrap">
                <div class="rs-field-top">
                  <label class="rs-field-label" for="rsFromInput">From station</label>
                  ${fromStation ? `<span class="rs-station-code-badge">${fromStation.code}</span>` : ""}
                </div>
                <input
                  id="rsFromInput"
                  class="rs-field-input"
                  type="text"
                  inputmode="text"
                  autocomplete="off"
                  spellcheck="false"
                  placeholder="From station (e.g. NDLS, New Delhi)"
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
              <div class="rs-connector-dots" aria-hidden="true">
                <span></span><span></span><span></span>
              </div>
              <div class="rs-connector-line" aria-hidden="true"></div>
              <button class="rs-swap-btn" id="rsSwapBtn" aria-label="Swap from and to stations" title="Swap stations">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M7 16V4m0 0L3 8m4-4 4 4"/><path d="M17 8v12m0 0 4-4m-4 4-4-4"/>
                </svg>
              </button>
            </div>

            <!-- To field -->
            <div class="rs-field-row rs-to-row">
              <div class="rs-field-icon rs-to-icon" aria-hidden="true">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/>
                  <circle cx="12" cy="10" r="3"/>
                </svg>
              </div>
              <div class="rs-field-input-wrap" id="rsToWrap">
                <div class="rs-field-top">
                  <label class="rs-field-label" for="rsToInput">To station</label>
                  ${toStation ? `<span class="rs-station-code-badge">${toStation.code}</span>` : ""}
                </div>
                <input
                  id="rsToInput"
                  class="rs-field-input"
                  type="text"
                  inputmode="text"
                  autocomplete="off"
                  spellcheck="false"
                  placeholder="To station (e.g. HWH, Howrah)"
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

        <!-- Autocomplete dropdown slots -->
        <div id="rsFromDropdownSlot">${fromDropOpen && fromSuggestions.length > 0 ? `
          <div class="rs-dropdown" id="rsFromDropdown" role="listbox" aria-label="Departure station suggestions">
            ${fromSuggestions.map(s => `
              <div class="rs-dropdown-item" data-code="${s.code}" data-name="${s.name}" data-field="from" role="option">
                <span class="rs-dropdown-code">${s.code}</span>
                <span class="rs-dropdown-name">${s.name}</span>
              </div>
            `).join("")}
          </div>
        ` : ""}</div>

        <div id="rsToDropdownSlot">${toDropOpen && toSuggestions.length > 0 ? `
          <div class="rs-dropdown rs-dropdown-to" id="rsToDropdown" role="listbox" aria-label="Destination station suggestions">
            ${toSuggestions.map(s => `
              <div class="rs-dropdown-item" data-code="${s.code}" data-name="${s.name}" data-field="to" role="option">
                <span class="rs-dropdown-code">${s.code}</span>
                <span class="rs-dropdown-name">${s.name}</span>
              </div>
            `).join("")}
          </div>
        ` : ""}</div>
      </div>

      <!-- Dynamic Live Train Results Container -->
      <div id="rsResultsContainer">
        ${renderRouteResultsContent()}
      </div>

    </section>
  `;
}

function renderRouteResultsContent() {
  if (isSearching) {
    return `
      <div class="rs-results-loading" aria-live="polite" aria-busy="true">
        <div class="rs-skeleton-card"></div>
        <div class="rs-skeleton-card"></div>
        <div class="rs-skeleton-card"></div>
      </div>
    `;
  }

  const { title, trains } = getTrainsToDisplay();

  // Filter by running day
  let filtered = trains;
  let targetDayIdx = null;
  if (dayFilter === "today") targetDayIdx = TODAY_IDX;
  else if (dayFilter === "tomorrow") targetDayIdx = TOMORROW_IDX;
  else if (dayFilter !== "all") targetDayIdx = parseInt(dayFilter, 10);

  if (targetDayIdx !== null) {
    filtered = filtered.filter(t => {
      const days = parseDays(t.runningDays || t.days);
      return days[targetDayIdx];
    });
  }

  // Filter by live filter string (matches any alphabet, word, number)
  if (liveFilterQuery.trim()) {
    const q = liveFilterQuery.trim().toLowerCase();
    filtered = filtered.filter(t => {
      return (t.number && t.number.toLowerCase().includes(q)) ||
             (t.name && t.name.toLowerCase().includes(q)) ||
             (t.origin && t.origin.toLowerCase().includes(q)) ||
             (t.destination && t.destination.toLowerCase().includes(q)) ||
             (t.type && t.type.toLowerCase().includes(q));
    });
  }

  return `
    <div class="rs-results" role="list" aria-label="Matching trains">

      <!-- Live Search & Filter Bar (Type any word, string, alphabet) -->
      <div class="rs-live-filter-box">
        <div class="rs-filter-input-wrap">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
          </svg>
          <input
            type="text"
            id="rsLiveFilterInput"
            class="rs-live-filter-input"
            placeholder="Search by train no., name, or any route word..."
            value="${liveFilterQuery}"
            autocomplete="off"
            spellcheck="false"
          />
          ${liveFilterQuery ? `<button id="rsClearLiveFilter" class="rs-filter-clear-btn" title="Clear search">✕</button>` : ""}
        </div>
      </div>

      <!-- Filter bar: Day Filter and Count -->
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
          Showing <strong>${filtered.length}</strong> trains
        </div>
      </div>

      <!-- Route Header card -->
      ${fromStation && toStation ? `
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
      ` : `
        <div style="font-size:13px;font-weight:700;color:var(--color-text-secondary);margin:8px 4px 12px;display:flex;align-items:center;gap:6px;">
          <span>🚆</span>
          <span>${title}</span>
        </div>
      `}

      <!-- Trains list -->
      <div id="rsTrainCardsList">
        ${filtered.length === 0 ? `
          <div class="rs-empty" role="status" style="padding:28px 16px;">
            <div class="rs-empty-icon" aria-hidden="true">🔍</div>
            <p class="rs-empty-title">No trains matching "${liveFilterQuery || fromQuery || toQuery}"</p>
            <p class="rs-empty-sub">Try searching by any train number (e.g. 12404, 22436), train name (Rajdhani, Vande Bharat), or station name.</p>
            ${liveFilterQuery ? `<button class="rs-reset-filter-btn" id="rsResetLiveFilterBtn">Clear Search</button>` : ""}
          </div>
        ` : filtered.map(t => {
          const days = parseDays(t.runningDays || t.days);
          const runsToday = days[TODAY_IDX];
          const depTime = fmtTime(t.departureTime || t.dep || t.departure);
          const arrTime = fmtTime(t.arrivalTime || t.arr || t.arrival);
          const duration = t.durationMinutes || t.duration;
          const originText = t.origin || (fromStation ? fromStation.name : "Origin");
          const destText = t.destination || (toStation ? toStation.name : "Destination");

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

              <!-- Route Endpoints -->
              <div style="font-size:12px;color:var(--color-text-muted);margin-bottom:8px;display:flex;align-items:center;gap:6px;">
                <span style="font-weight:600;color:var(--color-text-secondary);">${originText}</span>
                <span>→</span>
                <span style="font-weight:600;color:var(--color-text-secondary);">${destText}</span>
              </div>

              <!-- Timings row (if available) -->
              ${depTime !== "—" || arrTime !== "—" ? `
                <div class="rs-result-timing">
                  <div class="rs-time-block">
                    <span class="rs-time">${depTime}</span>
                    <span class="rs-time-stn-label">${fromStation ? fromStation.code : "DEP"}</span>
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
                    <span class="rs-time-stn-label">${toStation ? toStation.code : "ARR"}</span>
                  </div>
                </div>
              ` : ""}

              <!-- Week Calendar Routine (S M T W T F S) -->
              <div class="rs-calendar-wrap">
                <div class="rs-calendar-label">Weekly Schedule:</div>
                ${renderDaysBadges(days)}
              </div>

              <!-- Action footer -->
              <div class="rs-result-footer">
                ${t.type ? `<span class="rs-train-type-pill ${t.type?.toLowerCase().includes('pass') || t.type?.toLowerCase().includes('emu') ? 'rs-type-local' : ''}">${t.type}</span>` : ""}
                ${t.platform && t.platform !== "—"
                  ? `<span class="rs-platform-tag" style="background:#0284c7;color:#fff;font-weight:700;font-size:11px;padding:3px 8px;border-radius:4px;display:inline-flex;align-items:center;gap:4px;"><span>🚆</span><span>PF ${t.platform}</span></span>`
                  : `<span class="rs-platform-tag rs-platform-tag--expected" style="background:var(--color-bg-subtle);color:var(--color-text-muted);font-weight:600;font-size:11px;padding:3px 8px;border-radius:4px;border:1px dashed var(--color-border);display:inline-flex;align-items:center;gap:3px;"><span>PF</span><span>TBA</span></span>`
                }
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

function updateResultsOnly() {
  const container = document.getElementById("rsResultsContainer");
  if (!container) return;
  container.innerHTML = renderRouteResultsContent();
  attachResultItemEvents();
}

function updateDropdownOnly(field) {
  if (field === "from") {
    const slot = document.getElementById("rsFromDropdownSlot");
    if (!slot) return;
    slot.innerHTML = fromDropOpen && fromSuggestions.length > 0
      ? `<div class="rs-dropdown" id="rsFromDropdown" role="listbox" aria-label="Departure station suggestions">
          ${fromSuggestions.map(s => `
            <div class="rs-dropdown-item" data-code="${s.code}" data-name="${s.name}" data-field="from" role="option">
              <span class="rs-dropdown-code">${s.code}</span>
              <span class="rs-dropdown-name">${s.name}</span>
            </div>
          `).join("")}
        </div>`
      : "";
  } else {
    const slot = document.getElementById("rsToDropdownSlot");
    if (!slot) return;
    slot.innerHTML = toDropOpen && toSuggestions.length > 0
      ? `<div class="rs-dropdown rs-dropdown-to" id="rsToDropdown" role="listbox" aria-label="Destination station suggestions">
          ${toSuggestions.map(s => `
            <div class="rs-dropdown-item" data-code="${s.code}" data-name="${s.name}" data-field="to" role="option">
              <span class="rs-dropdown-code">${s.code}</span>
              <span class="rs-dropdown-name">${s.name}</span>
            </div>
          `).join("")}
        </div>`
      : "";
  }
  attachDropdownItemEvents();
}

async function doSearch() {
  if (!fromStation || !toStation) {
    const inp = !fromStation ? document.getElementById("rsFromInput") : document.getElementById("rsToInput");
    inp?.focus();
    inp?.classList.add("rs-input-shake");
    setTimeout(() => inp?.classList.remove("rs-input-shake"), 400);
    return;
  }

  isSearching = true;
  routeResults = [];
  updateResultsOnly();

  try {
    const results = await searchTrainsBetweenStations(fromStation.code, toStation.code);
    routeResults = results || [];
  } catch (err) {
    console.warn("[RouteSearch] search failed:", err.message);
    routeResults = [];
  } finally {
    isSearching = false;
    updateResultsOnly();
  }
}

function attachDropdownItemEvents() {
  document.querySelectorAll(".rs-dropdown-item").forEach(item => {
    item.addEventListener("click", () => {
      const code = item.getAttribute("data-code");
      const name = item.getAttribute("data-name");
      const field = item.getAttribute("data-field");
      if (field === "from") {
        fromStation = { code, name };
        fromQuery = name;
        fromDropOpen = false;
        rerenderSection();
        document.getElementById("rsToInput")?.focus();
      } else {
        toStation = { code, name };
        toQuery = name;
        toDropOpen = false;
        rerenderSection();
        if (fromStation && toStation) {
          doSearch();
        } else {
          document.getElementById("rsFindBtn")?.focus();
        }
      }
    });
  });
}

function attachResultItemEvents() {
  // Live filter input (matches any word, number, string)
  const filterInput = document.getElementById("rsLiveFilterInput");
  if (filterInput) {
    filterInput.addEventListener("input", (e) => {
      liveFilterQuery = e.target.value;
      updateResultsOnly();
      // Keep cursor focus in input
      const refreshedInput = document.getElementById("rsLiveFilterInput");
      if (refreshedInput) {
        refreshedInput.focus();
        const len = refreshedInput.value.length;
        refreshedInput.setSelectionRange(len, len);
      }
    });
  }

  document.getElementById("rsClearLiveFilter")?.addEventListener("click", () => {
    liveFilterQuery = "";
    updateResultsOnly();
  });

  document.getElementById("rsResetLiveFilterBtn")?.addEventListener("click", () => {
    liveFilterQuery = "";
    updateResultsOnly();
  });

  // Day filter select
  document.getElementById("rsDayFilter")?.addEventListener("change", e => {
    dayFilter = e.target.value;
    updateResultsOnly();
  });

  // Track Live button clicks
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
}

export function attachRouteSearchEvents() {
  // Swap button
  document.getElementById("rsSwapBtn")?.addEventListener("click", () => {
    [fromStation, toStation] = [toStation, fromStation];
    [fromQuery, toQuery] = [toQuery, fromQuery];
    fromDropOpen = false;
    toDropOpen = false;
    rerenderSection();
    if (fromStation && toStation) doSearch();
  });

  // Clear buttons
  document.getElementById("rsClearFrom")?.addEventListener("click", () => {
    fromStation = null;
    fromQuery = "";
    fromDropOpen = false;
    rerenderSection();
    document.getElementById("rsFromInput")?.focus();
  });

  document.getElementById("rsClearTo")?.addEventListener("click", () => {
    toStation = null;
    toQuery = "";
    toDropOpen = false;
    rerenderSection();
    document.getElementById("rsToInput")?.focus();
  });

  // Find trains button
  document.getElementById("rsFindBtn")?.addEventListener("click", doSearch);

  // From input
  const fromInput = document.getElementById("rsFromInput");
  fromInput?.addEventListener("input", e => {
    fromQuery = e.target.value;
    fromStation = null;
    clearTimeout(fromDebounce);
    fromDebounce = setTimeout(async () => {
      fromSuggestions = await getStationSuggestions(fromQuery);
      fromDropOpen = fromSuggestions.length > 0;
      updateDropdownOnly("from");
      // Dynamically update matching trains below as you type!
      updateResultsOnly();
    }, 180);
  });

  fromInput?.addEventListener("keydown", e => {
    if (e.key === "Enter") {
      if (fromSuggestions.length > 0) {
        const s = fromSuggestions[0];
        fromStation = s;
        fromQuery = s.name;
        fromDropOpen = false;
        rerenderSection();
        document.getElementById("rsToInput")?.focus();
      } else {
        document.getElementById("rsToInput")?.focus();
      }
    }
    if (e.key === "Escape") { fromDropOpen = false; updateDropdownOnly("from"); }
  });

  // To input
  const toInput = document.getElementById("rsToInput");
  toInput?.addEventListener("input", e => {
    toQuery = e.target.value;
    toStation = null;
    clearTimeout(toDebounce);
    toDebounce = setTimeout(async () => {
      toSuggestions = await getStationSuggestions(toQuery);
      toDropOpen = toSuggestions.length > 0;
      updateDropdownOnly("to");
      // Dynamically update matching trains below as you type!
      updateResultsOnly();
    }, 180);
  });

  toInput?.addEventListener("keydown", e => {
    if (e.key === "Enter") {
      if (toSuggestions.length > 0) {
        const s = toSuggestions[0];
        toStation = s;
        toQuery = s.name;
        toDropOpen = false;
        rerenderSection();
        if (fromStation && toStation) {
          doSearch();
        } else {
          document.getElementById("rsFindBtn")?.focus();
        }
      } else {
        doSearch();
      }
    }
    if (e.key === "Escape") { toDropOpen = false; updateDropdownOnly("to"); }
  });

  // Dropdown item events
  attachDropdownItemEvents();

  // Result item events
  attachResultItemEvents();

  // Close dropdowns on outside click
  document.addEventListener("click", e => {
    const insideFrom = document.getElementById("rsFromWrap")?.contains(e.target);
    const insideTo = document.getElementById("rsToWrap")?.contains(e.target);
    const insideDropFrom = document.getElementById("rsFromDropdown")?.contains(e.target);
    const insideDropTo = document.getElementById("rsToDropdown")?.contains(e.target);
    if (!insideFrom && !insideDropFrom && fromDropOpen) {
      fromDropOpen = false;
      updateDropdownOnly("from");
    }
    if (!insideTo && !insideDropTo && toDropOpen) {
      toDropOpen = false;
      updateDropdownOnly("to");
    }
  });
}
