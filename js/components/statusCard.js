/**
 * RailGaadi Live Train Status Card — v2
 * Fully data-driven from liveStatus + liveRoute from the store.
 * Fully responsive layout across mobile, tablet, and desktop viewports.
 */

import { store } from "../state/store.js";
import { trackerService } from "../services/trackerService.js";

/* ── Running Days Calendar ────────────────────────────────── */

const DAY_LABELS_FULL = ["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"];
const DAY_LABELS_SHORT = ["S","M","T","W","T","F","S"];
const TODAY_DAY_IDX = new Date().getDay(); // 0=Sun … 6=Sat

/** Parse any running-days format into a 7-element boolean array [Sun…Sat] */
function parseRunningDays(raw) {
  if (!raw) return null; // null = unknown (don't show calendar)
  if (typeof raw === "string" && raw.length === 7) {
    return raw.split("").map(c => c === "Y" || c === "1" || c === "y" || c === "T" || c === "t");
  }
  if (typeof raw === "number") {
    return DAY_LABELS_SHORT.map((_, i) => !!(raw & (1 << i)));
  }
  if (Array.isArray(raw)) {
    const lc = raw.map(d => String(d).toLowerCase().trim());
    return [
      ["sun","sunday","0"].some(a => lc.includes(a)),
      ["mon","monday","1"].some(a => lc.includes(a)),
      ["tue","tuesday","2"].some(a => lc.includes(a)),
      ["wed","wednesday","3"].some(a => lc.includes(a)),
      ["thu","thursday","4"].some(a => lc.includes(a)),
      ["fri","friday","5"].some(a => lc.includes(a)),
      ["sat","saturday","6"].some(a => lc.includes(a)),
    ];
  }
  if (typeof raw === "object") {
    return [
      !!(raw.sun || raw.sunday    || raw[0]),
      !!(raw.mon || raw.monday    || raw[1]),
      !!(raw.tue || raw.tuesday   || raw[2]),
      !!(raw.wed || raw.wednesday || raw[3]),
      !!(raw.thu || raw.thursday  || raw[4]),
      !!(raw.fri || raw.friday    || raw[5]),
      !!(raw.sat || raw.saturday  || raw[6]),
    ];
  }
  return null;
}

function renderRunningDaysCalendar(raw) {
  const days = parseRunningDays(raw);
  if (!days) return ""; // Don't show if we have no data

  const runsToday = days[TODAY_DAY_IDX];

  return `
    <div class="sc-days-calendar" aria-label="Running days schedule">
      <div class="sc-days-header">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4"/><path d="M8 2v4"/><path d="M3 10h18"/>
        </svg>
        <span>Runs on</span>
        <span class="sc-days-today-tag ${runsToday ? "sc-days-today-tag--yes" : "sc-days-today-tag--no"}">
          ${runsToday ? "✓ Running today" : "✗ Not today"}
        </span>
      </div>
      <div class="sc-days-pills">
        ${DAY_LABELS_SHORT.map((label, i) => `
          <span
            class="sc-day-pill ${days[i] ? "sc-day-pill--on" : "sc-day-pill--off"} ${i === TODAY_DAY_IDX ? "sc-day-pill--today" : ""}"
            title="${DAY_LABELS_FULL[i]}${days[i] ? "" : " (no service)"}"
            aria-label="${DAY_LABELS_FULL[i]}${days[i] ? " (runs)" : " (no service)"}"
          >${label}</span>
        `).join("")}
      </div>
    </div>
  `;
}

/* ── helpers ─────────────────────────────────────────────── */

function fmt(val, unit = "", fallback = "Unavailable") {
  if (val == null || val === "") return `<span style="color:var(--color-text-muted)">${fallback}</span>`;
  return `${val}${unit}`;
}

function formatTime(str) {
  if (!str || str === "—") return "—";
  if (typeof str === "string" && str.includes("T")) {
    try {
      const d = new Date(str);
      if (!isNaN(d.getTime())) {
        return d.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", hour12: true });
      }
    } catch {}
  }
  return str;
}

function timeDiff(isoString) {
  try {
    const diff = Math.floor((Date.now() - new Date(isoString).getTime()) / 1000);
    if (diff < 10 || isNaN(diff) || diff < 0) return "Just now · Live";
    if (diff < 60) return `${diff}s ago`;
    return `${Math.floor(diff / 60)}m ${diff % 60}s ago`;
  } catch { return "Live"; }
}

/* ── empty / landing state ──────────────────────────────── */

function renderLanding() {
  const state   = store.getState();
  const recents = state.recentSearches || [];

  return `
    <div class="card-panel landing-card-panel" style="padding:var(--space-4) var(--space-4);text-align:left;display:flex;flex-direction:column;gap:var(--space-3);">

      <!-- Quick Route Search Banner (Where is My Train style) -->
      <div class="landing-route-banner" id="landingRouteBanner" role="button" tabindex="0" style="background:linear-gradient(135deg, #1e3a8a 0%, #1e40af 100%);border-radius:var(--radius-lg);padding:14px 16px;color:#fff;cursor:pointer;display:flex;align-items:center;justify-content:space-between;box-shadow:0 4px 14px rgba(30,64,175,0.25);">
        <div style="display:flex;align-items:center;gap:12px;">
          <div style="width:38px;height:38px;border-radius:10px;background:rgba(255,255,255,0.18);display:flex;align-items:center;justify-content:center;flex-shrink:0;">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="6" cy="19" r="3"/><circle cx="18" cy="5" r="3"/><path d="M12 19h4.5a3.5 3.5 0 0 0 0-7h-8a3.5 3.5 0 0 1 0-7H12"/>
            </svg>
          </div>
          <div>
            <div style="font-weight:700;font-size:14px;letter-spacing:-0.01em;">Find Trains Between Stations</div>
            <div style="font-size:12px;opacity:0.85;">Station A → Station B &amp; Running Routine</div>
          </div>
        </div>
        <div style="background:rgba(255,255,255,0.22);padding:6px 10px;border-radius:20px;font-size:11px;font-weight:700;white-space:nowrap;display:flex;align-items:center;gap:4px;">
          <span>Find</span>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m9 18 6-6-6-6"/></svg>
        </div>
      </div>

      <!-- Search History Section -->
      ${recents.length > 0 ? `
        <div class="landing-section-wrap" style="margin-top:var(--space-1);">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px;">
            <div style="font-size:11px;font-weight:700;color:var(--color-text-secondary);text-transform:uppercase;letter-spacing:0.04em;display:flex;align-items:center;gap:6px;">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
              <span>Search History</span>
            </div>
            <button class="landing-clear-history-btn" id="clearSearchHistoryBtn" style="background:none;border:none;color:var(--color-danger, #ef4444);font-size:11px;font-weight:700;cursor:pointer;padding:3px 6px;border-radius:4px;display:flex;align-items:center;gap:4px;">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
              <span>Clear History</span>
            </button>
          </div>
          <div class="landing-recents-list" style="display:flex;flex-direction:column;gap:6px;">
            ${recents.map(trainId => {
              const meta = store.getTrainMeta(trainId);
              const num  = meta?.number || trainId;
              const name = meta?.name || `Train #${trainId}`;
              return `
                <div class="landing-recent-item landing-suggest-chip" data-train-id="${trainId}" style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;background:var(--color-bg-surface);border:1px solid var(--color-border);border-radius:var(--radius-md);cursor:pointer;transition:background 0.15s ease;">
                  <div style="display:flex;align-items:center;gap:10px;min-width:0;">
                    <span style="font-family:var(--font-mono);font-size:12px;font-weight:800;color:var(--color-accent);background:var(--color-accent-subtle);padding:3px 7px;border-radius:4px;flex-shrink:0;">${num}</span>
                    <span style="font-size:13px;font-weight:600;color:var(--color-text-primary);overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${name}</span>
                  </div>
                  <div style="display:flex;align-items:center;gap:4px;flex-shrink:0;">
                    <span style="font-size:11px;color:var(--color-accent);font-weight:600;">Track</span>
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--color-accent)" stroke-width="2.4"><path d="m9 18 6-6-6-6"/></svg>
                  </div>
                </div>
              `;
            }).join("")}
          </div>
        </div>
      ` : ""}

    </div>
  `;
}

/* ── skeleton loading state ─────────────────────────────── */

function renderSkeleton() {
  const shimmer = `background:linear-gradient(90deg,var(--color-bg-muted) 25%,var(--color-bg-subtle) 50%,var(--color-bg-muted) 75%);background-size:200% 100%;animation:shimmer 1.4s infinite;`;
  return `
    <style>@keyframes shimmer{0%{background-position:200% 0}100%{background-position:-200% 0}}</style>
    <div class="card-panel" aria-busy="true" aria-label="Loading train status">
      <div style="padding:var(--space-5);">
        <div style="display:flex;justify-content:space-between;margin-bottom:var(--space-4);">
          <div style="height:22px;width:120px;border-radius:var(--radius-full);${shimmer}"></div>
          <div style="height:22px;width:80px;border-radius:var(--radius-full);${shimmer}"></div>
        </div>
        <div style="height:28px;width:75%;border-radius:var(--radius-sm);margin-bottom:var(--space-2);${shimmer}"></div>
        <div style="height:16px;width:55%;border-radius:var(--radius-sm);${shimmer}"></div>
      </div>
      <div style="padding:var(--space-4) var(--space-5);background:var(--color-bg-subtle);border-bottom:1px solid var(--color-border-subtle);">
        <div style="height:8px;width:100%;border-radius:var(--radius-full);${shimmer}"></div>
      </div>
      <div style="padding:var(--space-5);display:flex;flex-direction:column;gap:var(--space-4);">
        ${[1,2].map(() => `
          <div style="display:flex;gap:var(--space-4);">
            <div style="width:18px;height:18px;border-radius:50%;flex-shrink:0;margin-top:2px;${shimmer}"></div>
            <div style="flex:1;">
              <div style="height:12px;width:40%;border-radius:4px;margin-bottom:8px;${shimmer}"></div>
              <div style="height:22px;width:70%;border-radius:4px;${shimmer}"></div>
            </div>
          </div>
        `).join("")}
      </div>
      <div style="display:grid;grid-template-columns:repeat(3,1fr);border-top:1px solid var(--color-border);">
        ${[1,2,3].map(() => `
          <div style="padding:var(--space-4) var(--space-3);border-right:1px solid var(--color-border);">
            <div style="height:12px;width:60%;margin:0 auto 8px;border-radius:4px;${shimmer}"></div>
            <div style="height:22px;width:70%;margin:0 auto;border-radius:4px;${shimmer}"></div>
          </div>
        `).join("")}
      </div>
    </div>
  `;
}

/* ── error state ─────────────────────────────────────────── */

function renderError(msg) {
  return `
    <div class="card-panel" style="padding:var(--space-8) var(--space-6);text-align:center;">
      <div style="font-size:36px;margin-bottom:var(--space-3);">📡</div>
      <h3 style="font-size:var(--text-md);font-weight:700;margin-bottom:var(--space-2);">Status Updating</h3>
      <p style="font-size:var(--text-sm);color:var(--color-text-secondary);margin-bottom:var(--space-5);">${msg}</p>
      <button class="header-btn" id="manualRetryBtn" style="margin:0 auto;">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/>
          <path d="M21 3v5h-5"/>
          <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/>
          <path d="M3 21v-5h5"/>
        </svg>
        <span>Retry Connection</span>
      </button>
    </div>
  `;
}

/* ── main card ───────────────────────────────────────────── */

export function renderStatusCard() {
  const state = store.getState();
  const { activeTrainId, isLoadingStatus, statusError, liveStatus, liveRoute,
          favourites, countdownSeconds, lastUpdatedAt } = state;

  if (!activeTrainId) return renderLanding();
  if (isLoadingStatus && !liveStatus) return renderSkeleton();
  if (statusError && !liveStatus) return renderError(statusError);

  const status      = liveStatus;
  const route       = liveRoute;
  const train       = state.activeTrain || {};
  const isFav       = favourites.includes(activeTrainId);
  const progressPct = trackerService.getProgressPercent() ?? 0;
  const curIdx      = trackerService.getCurrentStationIndex();

  const curStation  = status?.currentStation  || route?.stations?.[curIdx]       || null;
  const nextStation = status?.nextStation     || route?.stations?.[curIdx + 1]   || null;
  const totalKm     = route?.totalDistanceKm  || null;
  const coveredKm   = status?.distanceCoveredKm ?? (totalKm ? Math.round((progressPct / 100) * totalKm) : null);
  const remainingKm = status?.distanceRemainingKm ?? (totalKm && coveredKm != null ? Math.max(0, totalKm - coveredKm) : null);
  const speedKmh    = status?.speedKmh        ?? null;
  const delay       = status?.delayMinutes    ?? 0;
  const isStale     = status?.stale           || state.isStale;
  const updatedAt   = status?.updatedAt       || lastUpdatedAt?.toISOString();

  // Status badge
  let badgeClass = "on-time", badgeText = "ON TIME";
  if (status?.status === "NOT_STARTED") {
    badgeClass = "early";
    badgeText = "SCHEDULED";
  } else if (status?.status === "COMPLETED") {
    badgeClass = "on-time";
    badgeText = "ARRIVED";
  } else if (status?.status === "CANCELLED") {
    badgeClass = "delayed";
    badgeText = "CANCELLED";
  } else if (delay > 0) {
    badgeClass = "delayed";
    badgeText = `DELAYED ${delay} MIN`;
  } else if (delay < 0) {
    badgeClass = "early";
    badgeText = `EARLY ${Math.abs(delay)} MIN`;
  } else if (!status || status.status === "UNKNOWN") {
    badgeClass = "";
    badgeText = "RUNNING";
  }

  return `
    <article class="card-panel" aria-label="Live train running status">
      <!-- Stale Banner -->
      ${isStale ? `
        <div style="background:#fef3c7;border-bottom:1px solid #fde68a;padding:6px 16px;font-size:12px;font-weight:600;color:#92400e;display:flex;align-items:center;gap:6px;">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          Showing latest verified telemetry
        </div>
      ` : ""}

      <!-- Header -->
      <div class="status-card-header">
        <div class="status-badge-row">
          <div class="delay-badge ${badgeClass}">
            <span style="width:6px;height:6px;border-radius:50%;background:currentColor;display:inline-block;"></span>
            <span>${badgeText}</span>
          </div>
          <div class="freshness-tag">
            <span class="pulse-dot ${isStale ? "stale" : ""}"></span>
            <span>${timeDiff(updatedAt)}</span>
          </div>
        </div>

        <div class="train-headline">
          <h2 class="train-headline-name">${train.name || "Train #" + activeTrainId}</h2>
          <span class="train-headline-number">#${train.number || activeTrainId}</span>
        </div>

        <div class="train-endpoints">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          <span>${train.origin || route?.stations?.[0]?.name || "Origin"}</span>
          <span style="color:var(--color-text-muted)">→</span>
          <span>${train.destination || route?.stations?.[route?.stations?.length - 1]?.name || "Destination"}</span>
        </div>

        <!-- Running Days Calendar -->
        ${renderRunningDaysCalendar(liveStatus?.runningDays || train?.runningDays || liveStatus?.days || train?.days)}
      </div>

      <!-- Progress -->
      <div class="progress-card-section">
        <div class="progress-header">
          <span style="color:var(--color-text-secondary)">Journey Progress</span>
          <span style="font-family:var(--font-mono);font-weight:700;color:var(--color-accent);">${Math.round(progressPct)}%</span>
        </div>
        <div class="progress-track" role="progressbar" aria-valuenow="${Math.round(progressPct)}" aria-valuemin="0" aria-valuemax="100">
          <div class="progress-fill" style="width:${progressPct}%"></div>
        </div>
      </div>

      <!-- Station Spotlight -->
      <div class="station-spotlight-section">
        ${curStation ? `
          <div class="station-node">
            <div class="node-bullet">
              <div class="bullet-circle current"></div>
              <div class="bullet-line"></div>
            </div>
            <div class="station-details">
              <div class="station-badge-label current">Current / Last Reported Station</div>
              <div class="station-name-lg">${curStation.name} ${curStation.stationId ? `(${curStation.stationId})` : ""}</div>
              <div class="station-timing-row">
                ${curStation.scheduledArrival || curStation.scheduledArr ? `
                  <div class="timing-item">
                    <span class="timing-label">Scheduled</span>
                    <span class="timing-value">${formatTime(curStation.scheduledArrival || curStation.scheduledArr)}</span>
                  </div>
                ` : ""}
                ${curStation.actualArrival || curStation.actualArr || curStation.estimatedArrival ? `
                  <div class="timing-item">
                    <span class="timing-label">Reported</span>
                    <span class="timing-value ${delay > 0 ? "delayed-time" : ""}">${formatTime(curStation.actualArrival || curStation.actualArr || curStation.estimatedArrival)}</span>
                  </div>
                ` : ""}
                ${curStation.platform ? `
                  <div class="timing-item">
                    <span class="timing-label">Platform</span>
                    <span class="timing-value">PF ${curStation.platform}</span>
                  </div>
                ` : ""}
              </div>
            </div>
          </div>
        ` : ""}

        ${nextStation ? `
          <div class="station-node">
            <div class="node-bullet">
              <div class="bullet-circle"></div>
            </div>
            <div class="station-details">
              <div class="station-badge-label next">Next Halting Station</div>
              <div class="station-name-lg">${nextStation.name} ${nextStation.stationId ? `(${nextStation.stationId})` : ""}</div>
              <div class="station-timing-row">
                ${nextStation.scheduledArrival || nextStation.scheduledArr ? `
                  <div class="timing-item">
                    <span class="timing-label">Scheduled</span>
                    <span class="timing-value">${formatTime(nextStation.scheduledArrival || nextStation.scheduledArr)}</span>
                  </div>
                ` : ""}
                ${nextStation.estimatedArrival || nextStation.actualArr ? `
                  <div class="timing-item">
                    <span class="timing-label">Estimated ETA</span>
                    <span class="timing-value" style="color:var(--color-accent);">${formatTime(nextStation.estimatedArrival || nextStation.actualArr)}</span>
                  </div>
                ` : ""}
                ${nextStation.platform ? `
                  <div class="timing-item">
                    <span class="timing-label">Platform</span>
                    <span class="timing-value">PF ${nextStation.platform}</span>
                  </div>
                ` : ""}
                ${nextStation.haltMin ? `
                  <div class="timing-item">
                    <span class="timing-label">Halt</span>
                    <span class="timing-value">${nextStation.haltMin} min</span>
                  </div>
                ` : ""}
              </div>
            </div>
          </div>
        ` : (!curStation ? `<p style="padding:var(--space-4);font-size:var(--text-sm);color:var(--color-text-muted);">Station data syncing…</p>` : "")}
      </div>

      <!-- Metrics grid -->
      <div class="metrics-grid">
        <div class="metric-cell">
          <div class="metric-cell-label">Speed</div>
          <div class="metric-cell-value" style="color:var(--color-accent)">${fmt(speedKmh, " km/h", "—")}</div>
        </div>
        <div class="metric-cell">
          <div class="metric-cell-label">Covered</div>
          <div class="metric-cell-value">${fmt(coveredKm, " km", "—")}</div>
        </div>
        <div class="metric-cell">
          <div class="metric-cell-label">Remaining</div>
          <div class="metric-cell-value">${fmt(remainingKm, " km", "—")}</div>
        </div>
      </div>

      <!-- Actions -->
      <div class="status-card-actions">
        <div class="refresh-countdown">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/>
            <path d="M21 3v5h-5"/>
          </svg>
          <span>Refresh in <strong id="countdownDisplay">${countdownSeconds}s</strong></span>
        </div>
        <div class="status-actions-btns">
          <button class="header-btn" id="manualRefreshBtn">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/>
              <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M3 21v-5h5"/>
            </svg>
            <span>Refresh</span>
          </button>
          <button class="header-btn" id="cardFavToggleBtn">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="${isFav ? "#f59e0b" : "none"}" stroke="${isFav ? "#f59e0b" : "currentColor"}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
            <span>${isFav ? "Saved" : "Favourite"}</span>
          </button>
        </div>
      </div>
    </article>
  `;
}

export function attachStatusCardEvents() {
  // Landing suggest chips
  document.querySelectorAll(".landing-suggest-chip").forEach(chip => {
    chip.addEventListener("click", () => {
      const trainId = chip.getAttribute("data-train-id");
      if (trainId) trackerService.loadTrain(trainId);
    });
  });

  // Banner shortcut to Routes tab
  const routeBanner = document.getElementById("landingRouteBanner");
  if (routeBanner) {
    routeBanner.addEventListener("click", () => store.setActiveTab("routes"));
  }

  // Clear search history button
  const clearBtn = document.getElementById("clearSearchHistoryBtn");
  if (clearBtn) {
    clearBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      store.clearRecentSearches();
    });
  }

  const refreshBtn = document.getElementById("manualRefreshBtn");
  if (refreshBtn) {
    refreshBtn.addEventListener("click", () => {
      trackerService.triggerRefresh();
      store.showToast("Refreshing live telemetry…");
    });
  }

  const retryBtn = document.getElementById("manualRetryBtn");
  if (retryBtn) {
    retryBtn.addEventListener("click", () => {
      const id = store.getState().activeTrainId;
      if (id) trackerService.loadTrain(id);
    });
  }

  const favBtn = document.getElementById("cardFavToggleBtn");
  if (favBtn) {
    favBtn.addEventListener("click", () => {
      store.toggleFavourite(store.getState().activeTrainId);
    });
  }
}
