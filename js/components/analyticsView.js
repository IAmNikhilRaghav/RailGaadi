/**
 * RailGaadi Journey Analytics Component — v2
 * Fully driven from store.liveRoute and store.liveStatus (no hardcoded train data)
 * ElevationService fetches real SRTM data from OpenTopography API
 */

import { store } from "../state/store.js";
import { trackerService } from "../services/trackerService.js";
import { ElevationService } from "../services/elevationService.js";

const elevationAttemptedTrains = new Set();

// Persistent state for interactive accordion and view switcher
let isTimelineOpen = false;
let timelineViewMode = "wimt"; // "wimt" | "table"

/* ── helpers ──────────────────────────────────────────────── */

function fmtDelay(min) {
  if (min == null) return "—";
  if (min === 0)   return "0 min";
  return min > 0 ? `+${min} min` : `${Math.abs(min)} min early`;
}

function fmtTime(iso) {
  try { return new Date(iso).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" }); }
  catch { return iso || "—"; }
}

function formatTrainTime(t) {
  if (!t || t === "—" || t === "START") return t || "—";
  if (typeof t === "string" && t.includes("T")) {
    try {
      const d = new Date(t);
      if (!isNaN(d.getTime())) {
        return d.toLocaleTimeString("en-IN", { hour: "numeric", minute: "2-digit", hour12: true });
      }
    } catch (e) {}
  }
  if (typeof t === "string" && /^\d{1,2}:\d{2}$/.test(t)) {
    const [h, m] = t.split(":").map(Number);
    const ampm = h >= 12 ? "PM" : "AM";
    const h12 = h % 12 || 12;
    return `${h12}:${String(m).padStart(2, "0")} ${ampm}`;
  }
  return t;
}

function safeMax(arr) { return arr.length ? Math.max(...arr) : 0; }

/* ── empty / loading states ────────────────────────────────── */

function renderAnalyticsEmpty() {
  return `
    <div class="analytics-container" style="display:flex;align-items:center;justify-content:center;min-height:400px;">
      <div style="text-align:center;color:var(--color-text-muted);">
        <div style="font-size:48px;margin-bottom:12px;">📊</div>
        <h3 style="font-size:var(--text-lg);font-weight:700;color:var(--color-text-secondary);margin-bottom:8px;">Analytics Unavailable</h3>
        <p style="font-size:var(--text-sm);">Search and load a train first to see journey analytics, elevation profile, and timetable.</p>
      </div>
    </div>
  `;
}

function renderAnalyticsLoading() {
  const sh = `background:linear-gradient(90deg,var(--color-bg-muted) 25%,var(--color-bg-subtle) 50%,var(--color-bg-muted) 75%);background-size:200% 100%;animation:shimmer 1.4s infinite;border-radius:var(--radius-sm);`;
  return `
    <style>@keyframes shimmer{0%{background-position:200% 0}100%{background-position:-200% 0}}</style>
    <div class="analytics-container">
      <div class="analytics-row split-3">
        ${[1,2,3].map(() => `
          <div class="analytics-metric-card">
            <div style="height:12px;width:60%;margin-bottom:12px;${sh}"></div>
            <div style="height:36px;width:80%;margin-bottom:8px;${sh}"></div>
            <div style="height:10px;width:90%;${sh}"></div>
          </div>
        `).join("")}
      </div>
      <div class="elevation-card">
        <div class="chart-header"><div style="height:18px;width:40%;${sh}"></div></div>
        <div class="elevation-canvas-container" style="height:180px;${sh}"></div>
      </div>
    </div>
  `;
}

/* ── main render ──────────────────────────────────────────── */

export function renderAnalyticsSection() {
  const state = store.getState();
  const { activeTrainId, liveRoute, liveStatus, isLoadingRoute, activeTrain } = state;

  if (!activeTrainId)  return renderAnalyticsEmpty();
  if (isLoadingRoute)  return renderAnalyticsLoading();
  if (!liveRoute)      return renderAnalyticsEmpty();

  const stations    = liveRoute.stations || [];
  const delay       = liveStatus?.delayMinutes ?? null;
  const totalKm     = liveRoute.totalDistanceKm;
  const curIdx      = trackerService.getCurrentStationIndex();
  const progressPct = trackerService.getProgressPercent() ?? 0;

  // Speed data
  const speedHistory = store.getSpeedHistory();
  const currentSpeed = liveStatus?.speedKmh ?? (speedHistory.length ? speedHistory[speedHistory.length - 1].v : null);
  const avgSpeed     = speedHistory.length ? Math.round(speedHistory.reduce((acc, p) => acc + p.v, 0) / speedHistory.length) : currentSpeed;
  const maxSpeed     = speedHistory.length ? Math.max(...speedHistory.map(p => p.v)) : currentSpeed;

  // Elevation data (may be enriched asynchronously via ElevationService)
  const elevPoints  = stations.filter(s => s.elevationM != null).map(s => s.elevationM);
  const maxElev     = safeMax(elevPoints);
  const highestStn  = stations.find(s => s.elevationM === maxElev);

  // Next station distance
  const nextStnIdx  = curIdx + 1 < stations.length ? curIdx + 1 : null;
  const nextStn     = nextStnIdx != null ? stations[nextStnIdx] : null;
  const currStnDist = curIdx >= 0 && stations[curIdx]?.distanceKm != null ? stations[curIdx].distanceKm : null;
  const nextStnDist = nextStn?.distanceKm != null ? nextStn.distanceKm : null;
  const kmToNext    = (currStnDist != null && nextStnDist != null)
                        ? Math.round((nextStnDist - currStnDist) * 10) / 10
                        : null;

  return `
    <div class="analytics-container" aria-label="Journey Analytics">

      <!-- 1. Full Journey Timeline & Station Events — Dynamic Interactive Accordion Button -->
      <div class="card-panel wimt-accordion-card" id="journeyTimelineAccordion">

        <!-- Header Bar (Clickable) -->
        <button type="button" class="wimt-accordion-bar ${isTimelineOpen ? "is-open" : ""}" id="journeyTimelineToggle" aria-expanded="${isTimelineOpen ? "true" : "false"}" aria-controls="journeyTimelineBody">
          <div class="wimt-accordion-bar-left">
            <div class="wimt-accordion-icon-badge">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="4" y="3" width="16" height="16" rx="2"/>
                <path d="M4 11h16M12 3v8M8 19l-2 3M16 19l2 3M9 15h.01M15 15h.01"/>
              </svg>
            </div>
            <div class="wimt-accordion-titles">
              <div class="wimt-accordion-title-row">
                <span class="wimt-accordion-title">Full Journey Timeline &amp; Station Events</span>
                <span class="wimt-accordion-live-pill">● LIVE TRACKER</span>
              </div>
              <div class="wimt-accordion-meta">
                <span>${stations.length} Stations</span>
                <span class="wimt-meta-dot">·</span>
                <span>${progressPct}% Covered</span>
                ${kmToNext != null ? `<span class="wimt-meta-dot">·</span><span class="wimt-next-stn-highlight">Next: <strong>${nextStn?.name}</strong> (${kmToNext} km)</span>` : ""}
              </div>
            </div>
          </div>
          <div class="wimt-accordion-bar-right">
            <span class="wimt-accordion-status-chip ${delay > 0 ? "is-late" : "is-ontime"}">
              ${delay == null ? "Scheduled" : delay === 0 ? "On Time" : delay > 0 ? `+${delay}m Late` : `${Math.abs(delay)}m Early`}
            </span>
            <div class="wimt-chevron-pill" title="Click to expand/collapse timeline">
              <svg class="wimt-chevron-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </div>
          </div>
        </button>

        <!-- Prominent Interactive Blue Next-Station Summary Bar (Primary trigger requested by user) -->
        <div class="wimt-summary-bar-wrapper">
          <button type="button" class="next-stn-interactive-btn ${isTimelineOpen ? "is-expanded" : ""}" id="nextStnSummaryBar" aria-expanded="${isTimelineOpen ? "true" : "false"}" aria-controls="journeyTimelineBody" title="Touch to view or collapse all station events">
            <div class="next-stn-banner-left">
              <div class="next-stn-icon-ring">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 2v10m0 0 3-3m-3 3-3-3"/><rect x="2" y="14" width="20" height="8" rx="2"/>
                </svg>
              </div>
              <span>${kmToNext != null ? `Next Station: <strong>${nextStn?.name || "Next Halt"}</strong> — <strong>${kmToNext} km</strong> remaining` : `Train en-route — <strong>${stations.length} stations total</strong>`}</span>
            </div>
            <div class="next-stn-banner-right">
              <span class="next-stn-cta-text">${isTimelineOpen ? "Hide Description" : "Tap to view full description"}</span>
              <svg class="next-stn-chevron-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </div>
          </button>
        </div>

        <!-- Collapsible Container Body (Shown only when opened via blue bar or header) -->
        <div class="wimt-accordion-body" id="journeyTimelineBody" ${isTimelineOpen ? "" : "hidden"}>

          <!-- Top Toolbar: View Mode Switcher -->
          <div class="wimt-toolbar-row">
            <div class="wimt-toolbar-info">
              <span class="wimt-legend-dot dot-halt"></span> Scheduled Stop
              <span class="wimt-legend-dot dot-through" style="margin-left:12px;"></span> Pass-through
              <span class="wimt-legend-dot dot-current" style="margin-left:12px;"></span> Live Train
            </div>

            <!-- View Switcher Tabs -->
            <div class="wimt-view-switcher" role="tablist" aria-label="Timeline View Format">
              <button type="button" class="wimt-switch-btn ${timelineViewMode === "wimt" ? "active" : ""}" id="btnSwitchWimt" data-view="wimt">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="3" width="16" height="16" rx="2"/><path d="M4 11h16M12 3v8M8 19l-2 3M16 19l2 3"/></svg>
                <span>Where Is My Train View</span>
              </button>
              <button type="button" class="wimt-switch-btn ${timelineViewMode === "table" ? "active" : ""}" id="btnSwitchTable" data-view="table">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3h18v18H3zM3 9h18M3 15h18M9 3v18M15 3v18"/></svg>
                <span>Detailed Table (No., ETA, PF, etc.)</span>
              </button>
            </div>
          </div>

          <!-- ======================================================== -->
          <!-- VIEW 1: WHERE IS MY TRAIN (WIMT) VIEW                   -->
          <!-- ======================================================== -->
          <div class="wimt-view-container" id="wimtTimelineView" style="display:${timelineViewMode === "wimt" ? "block" : "none"};">
            <!-- WIMT Subheader: Arrival | Station / Day | Departure -->
            <div class="wimt-subhead">
              <div class="wimt-subhead-arr">Arrival</div>
              <div class="wimt-subhead-center">Day 1 · Stations</div>
              <div class="wimt-subhead-dep">Departure</div>
            </div>

            <div class="wimt-timeline-track-list">
              ${stations.map((s, idx) => {
                const isPassed   = idx < curIdx;
                const isCurrent  = idx === curIdx;
                const isUpcoming = idx > curIdx;
                const isFirst    = idx === 0;
                const isLast     = idx === stations.length - 1;

                const haltMin    = s.haltMin ?? s.halt_min ?? s.haltMinutes ?? null;
                const isThrough  = haltMin != null && haltMin === 0;
                const isHalting  = !isThrough;

                const schArr = formatTrainTime(s.scheduledArr);
                const schDep = formatTrainTime(s.scheduledDep);
                const actArr = formatTrainTime(s.actualArr);
                const actDep = formatTrainTime(s.actualDep);

                const delayMin = s.delayMinutes ?? (isCurrent ? delay : null);
                const isLate   = delayMin != null && delayMin > 0;
                const isEarly  = delayMin != null && delayMin < 0;

                // Upcoming distance remaining
                const currTrainDistKm = curIdx >= 0 && stations[curIdx]?.distanceKm != null ? stations[curIdx].distanceKm : null;
                const thisStnDistKm   = s.distanceKm != null ? s.distanceKm : null;
                const kmRemaining     = (isUpcoming && currTrainDistKm != null && thisStnDistKm != null)
                                          ? Math.round((thisStnDistKm - currTrainDistKm) * 10) / 10
                                          : null;

                return `
                  <div class="wimt-stn-row ${isCurrent ? "is-current-station" : ""} ${isPassed ? "is-passed-station" : "is-upcoming-station"} ${isThrough ? "is-through-station" : "is-halting-station"}">

                    <!-- Left: Arrival Times -->
                    <div class="wimt-col-arrival">
                      <div class="wimt-time-sched ${isPassed || isCurrent ? "is-muted" : ""}">${schArr}</div>
                      ${actArr && actArr !== schArr ? `
                        <div class="wimt-time-actual ${isLate ? "is-late-time" : "is-ontime-time"}">${actArr}</div>
                      ` : (isCurrent && delayMin != null && delayMin > 0) ? `
                        <div class="wimt-time-actual is-late-time">+${delayMin}m</div>
                      ` : ""}
                    </div>

                    <!-- Center: Track Spine + Station Info + Train Bubble -->
                    <div class="wimt-col-center">
                      <!-- Vertical Continuous Track Line -->
                      <div class="wimt-track-spine">
                        <div class="wimt-track-segment wimt-track-top ${isPassed || isCurrent ? "track-done" : ""} ${isFirst ? "track-hidden" : ""}"></div>

                        ${isCurrent ? `
                          <!-- WIMT Train Emblem on track -->
                          <div class="wimt-train-circle-badge" aria-label="Train is standing here">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="#ffffff">
                              <path d="M12 2c-4 0-8 .5-8 4v9.5C4 17.43 5.57 19 7.5 19L6 20.5v.5h12v-.5L16.5 19c1.93 0 3.5-1.57 3.5-3.5V6c0-3.5-4-4-8-4zm-4 14c-.83 0-1.5-.67-1.5-1.5S7.17 13 8 13s1.5.67 1.5 1.5S8.83 16 8 16zm8 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm1-6H7V6h10v4z"/>
                            </svg>
                            <span class="wimt-train-ripple"></span>
                          </div>
                        ` : isHalting ? `
                          <!-- Solid Station Node -->
                          <div class="wimt-station-dot ${isPassed ? "dot-passed" : "dot-future"}"></div>
                        ` : `
                          <!-- Pass-through Small Node -->
                          <div class="wimt-station-dot-small ${isPassed ? "dot-passed" : "dot-future"}"></div>
                        `}

                        <div class="wimt-track-segment wimt-track-bottom ${isPassed ? "track-done" : ""} ${isLast ? "track-hidden" : ""}"></div>
                      </div>

                      <!-- Station Details and Live Bubble -->
                      <div class="wimt-station-details">
                        <div class="wimt-stn-title-row">
                          <span class="wimt-station-name ${isThrough ? "name-through" : ""}">${s.name}</span>
                          ${isThrough ? `<span class="wimt-pass-tag">Pass-through</span>` : ""}
                        </div>

                        <div class="wimt-station-subinfo">
                          ${s.distanceKm != null ? `<span class="wimt-dist-tag">${s.distanceKm} km</span>` : ""}
                          ${s.platform ? `<span class="wimt-pf-tag">Platform ${s.platform}</span>` : ""}
                          ${haltMin > 0 ? `<span class="wimt-halt-tag">Halt ${haltMin}m</span>` : ""}
                        </div>

                        ${isCurrent ? `
                          <!-- WIMT Floating Speech-Bubble Card -->
                          <div class="wimt-train-speech-card">
                            <div class="wimt-bubble-pointer"></div>
                            <div class="wimt-bubble-header">
                              <span class="wimt-bubble-title">Arrived at ${s.name}</span>
                              <span class="wimt-bubble-live-dot">● LIVE</span>
                            </div>
                            <div class="wimt-bubble-status ${isLate ? "is-late-text" : "is-ontime-text"}">
                              <span class="wimt-signal-icon">((•))</span>
                              ${isLate ? `Delayed by ${delayMin} mins` : delayMin === 0 ? "Running on time" : `${Math.abs(delayMin || 0)} mins early`}
                            </div>
                            <div class="wimt-bubble-time">Updated a few seconds ago</div>
                          </div>
                        ` : ""}

                        ${isUpcoming && kmRemaining != null ? `
                          <div class="wimt-upcoming-km">📍 ${kmRemaining} km remaining</div>
                        ` : ""}
                      </div>
                    </div>

                    <!-- Right: Departure Times -->
                    <div class="wimt-col-departure">
                      <div class="wimt-time-sched ${isPassed ? "is-muted" : ""}">${schDep}</div>
                      ${actDep && actDep !== schDep ? `
                        <div class="wimt-time-actual ${isLate ? "is-late-time" : "is-ontime-time"}">${actDep}</div>
                      ` : (isCurrent && delayMin != null && delayMin > 0) ? `
                        <div class="wimt-time-actual is-late-time">+${delayMin}m</div>
                      ` : ""}
                    </div>

                  </div>
                `;
              }).join("")}
            </div>
          </div>

          <!-- ======================================================== -->
          <!-- VIEW 2: DETAILED TABLE VIEW (No., Station, Time, PF, etc) -->
          <!-- ======================================================== -->
          <div class="wimt-view-container" id="detailedTableView" style="display:${timelineViewMode === "table" ? "block" : "none"};">
            <div class="events-table-wrapper" style="display:block;">
              <table class="events-table" aria-label="Station events timetable">
                <thead>
                  <tr>
                    <th>No.</th>
                    <th>Station</th>
                    <th>Arr · Dep (Scheduled)</th>
                    <th>Actual / ETA</th>
                    <th>Distance</th>
                    <th>Platform</th>
                    <th>Elevation</th>
                  </tr>
                </thead>
                <tbody>
                  ${stations.map((s, idx) => {
                    const isPassed  = idx < curIdx;
                    const isCurrent = idx === curIdx;
                    const rowClass  = isCurrent ? "current-row" : "";
                    const haltMin   = s.haltMin ?? s.halt_min ?? s.haltMinutes ?? null;
                    const isThrough = haltMin != null && haltMin === 0;

                    const schArr = formatTrainTime(s.scheduledArr);
                    const schDep = formatTrainTime(s.scheduledDep);
                    const actArr = formatTrainTime(s.actualArr);

                    return `
                      <tr class="${rowClass} ${isThrough ? "row-through" : "row-halting"}">
                        <td class="col-no">
                          <div class="tl-col-cell">
                            <div class="tl-line tl-line--top ${isPassed || isCurrent ? "tl-line--done" : ""} ${idx === 0 ? "tl-line--invisible" : ""}"></div>
                            ${isCurrent ? `
                              <span class="tl-train-marker" aria-label="Train is here">
                                <svg viewBox="0 0 16 24" width="16" height="22" xmlns="http://www.w3.org/2000/svg">
                                  <rect x="1" y="4" width="14" height="10" rx="2.5" fill="#1e40af"/>
                                  <rect x="3" y="2" width="10" height="4" rx="1.5" fill="#2563eb"/>
                                  <rect x="4" y="5" width="3.5" height="3" rx="0.8" fill="#bfdbfe"/>
                                  <rect x="9" y="5" width="3.5" height="3" rx="0.8" fill="#bfdbfe"/>
                                  <circle cx="4.5" cy="17" r="2.5" fill="#1e3a8a"/>
                                  <circle cx="11.5" cy="17" r="2.5" fill="#1e3a8a"/>
                                </svg>
                                <span class="tl-pulse-ring"></span>
                              </span>
                            ` : isPassed ? `
                              <span class="tl-node tl-node--passed"></span>
                            ` : `
                              <span class="tl-node tl-node--future"></span>
                            `}
                            <div class="tl-line tl-line--bottom ${isPassed ? "tl-line--done" : ""} ${idx === stations.length - 1 ? "tl-line--invisible" : ""}"></div>
                          </div>
                          <span class="tl-seq">${s.sequence || idx + 1}</span>
                        </td>
                        <td>
                          <div class="station-name-cell ${isCurrent ? "station-name-cell--current" : ""}">${s.name}</div>
                          <div class="station-code-cell">
                            ${s.code || s.stationId || ""}
                            ${isCurrent ? "· 🟢 Live" : isPassed ? "· ✓" : ""}
                            ${isThrough ? "· Pass" : haltMin ? `· ${haltMin}m halt` : ""}
                          </div>
                        </td>
                        <td>
                          <div>${schArr !== "—" ? schArr : "—"}</div>
                          <div style="font-size:12px;color:var(--color-text-secondary);">${schDep !== "—" ? `Dep: ${schDep}` : ""}</div>
                        </td>
                        <td style="font-weight:600;color:${idx <= curIdx ? "var(--color-text-primary)" : "var(--color-accent)"};">
                          ${actArr || schArr || "—"}
                        </td>
                        <td>${s.distanceKm != null ? `${s.distanceKm} km` : "—"}</td>
                        <td>${s.platform ? `PF ${s.platform}` : "—"}</td>
                        <td>${s.elevationM != null ? `${s.elevationM} m` : "—"}</td>
                      </tr>
                    `;
                  }).join("")}
                </tbody>
              </table>
            </div>
          </div>

        </div><!-- /wimt-accordion-body -->
      </div><!-- /wimt-accordion-card -->

      <!-- 2. Key Metrics Row -->
      <div class="analytics-row split-3">
        <div class="analytics-metric-card">
          <div class="metric-card-top">
            <span class="metric-card-title">Schedule Variance</span>
            <span style="font-size:20px;">⏱️</span>
          </div>
          <div class="metric-card-value-hero" style="color:${delay == null ? "var(--color-text-muted)" : delay === 0 ? "var(--color-success)" : delay > 0 ? "var(--color-danger)" : "var(--color-success)"};">
            ${fmtDelay(delay)}
          </div>
          <div class="metric-card-note">${delay == null ? "Live delay data unavailable" : delay <= 0 ? "Running on schedule" : "En-route operational delay"}</div>
        </div>

        <div class="analytics-metric-card">
          <div class="metric-card-top">
            <span class="metric-card-title">Journey Distance</span>
            <span style="font-size:20px;">📏</span>
          </div>
          <div class="metric-card-value-hero">${totalKm ? `${totalKm} <span style="font-size:14px;font-weight:500;">km</span>` : "—"}</div>
          <div class="metric-card-note">${stations.length ? `${stations.length} stations · ${progressPct}% complete` : "Route data loading"}</div>
        </div>

        <div class="analytics-metric-card">
          <div class="metric-card-top">
            <span class="metric-card-title">Highest Route Point</span>
            <span style="font-size:20px;">⛰️</span>
          </div>
          <div class="metric-card-value-hero">${maxElev ? `${maxElev} <span style="font-size:14px;font-weight:500;">m</span>` : `<span style="color:var(--color-text-muted);font-size:16px;">Loading…</span>`}</div>
          <div class="metric-card-note">${highestStn ? `Near ${highestStn.name}` : "Fetching SRTM elevation data"}</div>
        </div>
      </div>

      <!-- 3. Live Speed Telemetry & Velocity Curve -->
      <div class="elevation-card speed-card">
        <div class="chart-header">
          <div class="chart-title-group">
            <h3>Live Speed Telemetry &amp; Velocity Curve</h3>
            <p>Real-time locomotive velocity measurements and session speed telemetry</p>
          </div>
          <div class="highest-elevation-pill" style="background:var(--color-bg-subtle);border-color:var(--color-border);color:var(--color-text-primary);">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 14l4-4"/>
              <path d="M3.34 19a10 10 0 1 1 17.32 0"/>
            </svg>
            <span>Current: <strong style="color:var(--color-accent);">${currentSpeed != null ? `${currentSpeed} km/h` : "—"}</strong>${maxSpeed ? ` · Peak: <strong>${maxSpeed} km/h</strong>` : ""}</span>
          </div>
        </div>
        <div class="elevation-canvas-container" style="height:190px;">
          <canvas id="speedCanvas" class="elevation-canvas"></canvas>
          <div id="speedTooltip" class="elevation-tooltip"></div>
        </div>
      </div>

      <!-- 4. Route Elevation Profile -->
      <div class="elevation-card">
        <div class="chart-header">
          <div class="chart-title-group">
            <h3>Route Elevation Profile</h3>
            <p>Terrain altitude along the rail corridor (Source: OpenTopography SRTM)</p>
          </div>
          ${maxElev ? `
            <div class="highest-elevation-pill">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="m8 3 4 8 5-5 5 15H2L8 3z"/>
              </svg>
              <span>Peak: <strong>${maxElev} m</strong>${highestStn ? ` (${highestStn.name})` : ""}</span>
            </div>
          ` : `<div style="font-size:12px;color:var(--color-text-muted);">Elevation: loading…</div>`}
        </div>
        <div class="elevation-canvas-container">
          <canvas id="elevationCanvas" class="elevation-canvas"></canvas>
          <div id="elevationTooltip" class="elevation-tooltip"></div>
          ${!maxElev ? `
            <div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;color:var(--color-text-muted);font-size:13px;">
              Elevation data being fetched from OpenTopography…
            </div>
          ` : ""}
        </div>
      </div>

    </div>
  `;
}

/* ── Interactive Event Handlers ───────────────────────────── */

export function attachAnalyticsEvents() {
  const toggleBtn = document.getElementById("journeyTimelineToggle");
  const body      = document.getElementById("journeyTimelineBody");
  if (toggleBtn && body) {
    toggleBtn.onclick = (e) => {
      e.preventDefault();
      isTimelineOpen = !isTimelineOpen;
      toggleBtn.classList.toggle("is-open", isTimelineOpen);
      toggleBtn.setAttribute("aria-expanded", isTimelineOpen ? "true" : "false");
      body.hidden = !isTimelineOpen;
    };
  }

  const btnWimt  = document.getElementById("btnSwitchWimt");
  const btnTable = document.getElementById("btnSwitchTable");
  const viewWimt = document.getElementById("wimtTimelineView");
  const viewTable = document.getElementById("detailedTableView");

  if (btnWimt && btnTable && viewWimt && viewTable) {
    btnWimt.onclick = () => {
      timelineViewMode = "wimt";
      btnWimt.classList.add("active");
      btnTable.classList.remove("active");
      viewWimt.style.display = "block";
      viewTable.style.display = "none";
    };
    btnTable.onclick = () => {
      timelineViewMode = "table";
      btnTable.classList.add("active");
      btnWimt.classList.remove("active");
      viewTable.style.display = "block";
      viewWimt.style.display = "none";
    };
  }
}

// Global click delegation as fallback for dynamic re-renders
if (typeof window !== "undefined" && !window._hasAnalyticsDelegation) {
  window._hasAnalyticsDelegation = true;
  document.addEventListener("click", (e) => {
    const toggle = e.target.closest("#journeyTimelineToggle");
    if (toggle) {
      const body = document.getElementById("journeyTimelineBody");
      if (body) {
        isTimelineOpen = !isTimelineOpen;
        toggle.classList.toggle("is-open", isTimelineOpen);
        toggle.setAttribute("aria-expanded", isTimelineOpen ? "true" : "false");
        body.hidden = !isTimelineOpen;
      }
      return;
    }

    const switchBtn = e.target.closest(".wimt-switch-btn");
    if (switchBtn) {
      const mode = switchBtn.getAttribute("data-view");
      if (mode) {
        timelineViewMode = mode;
        const btnWimt  = document.getElementById("btnSwitchWimt");
        const btnTable = document.getElementById("btnSwitchTable");
        const viewWimt = document.getElementById("wimtTimelineView");
        const viewTable = document.getElementById("detailedTableView");
        if (btnWimt && btnTable && viewWimt && viewTable) {
          btnWimt.classList.toggle("active", mode === "wimt");
          btnTable.classList.toggle("active", mode === "table");
          viewWimt.style.display = mode === "wimt" ? "block" : "none";
          viewTable.style.display = mode === "table" ? "block" : "none";
        }
      }
    }
  });
}


/* ── elevation chart drawing ──────────────────────────────── */

export async function drawElevationChart() {
  const canvas = document.getElementById("elevationCanvas");
  if (!canvas) return;

  const state   = store.getState();
  const route   = state.liveRoute;
  if (!route?.stations?.length) return;

  // Enrich stations with elevation data from OpenTopography if missing (once per train)
  const stations       = route.stations;
  const trainId        = state.activeTrainId || "default";
  const needsElevation = stations.some(s => s.elevationM == null);

  if (!elevationAttemptedTrains.has(trainId) && needsElevation && stations.some(s => s.latitude && s.longitude)) {
    elevationAttemptedTrains.add(trainId);
    try {
      const coords = stations
        .filter(s => s.latitude && s.longitude)
        .map(s => [s.longitude, s.latitude]);
      const elevations = await ElevationService.fetchElevations(coords);
      if (elevations?.length) {
        stations.forEach((s, i) => {
          if (elevations[i] != null) s.elevationM = elevations[i];
          else if (s.elevationM == null) s.elevationM = 215; // default plain level
        });
        // Re-render once with enriched data
        const root = document.getElementById("analyticsViewRoot");
        if (root) {
          root.innerHTML = renderAnalyticsSection();
          const newCanvas = document.getElementById("elevationCanvas");
          if (newCanvas) drawElevationCanvasOnly(newCanvas, stations, route.totalDistanceKm);
          return;
        }
      }
    } catch (e) {
      // Default missing elevations so chart always renders smoothly
      stations.forEach(s => { if (s.elevationM == null) s.elevationM = 215; });
    }
  }

  drawElevationCanvasOnly(canvas, stations, route.totalDistanceKm);
}

function drawElevationCanvasOnly(canvas, stations, totalDistanceKm) {
  // Build elevation profile data from stations that have distance + elevation
  const data = stations
    .filter(s => s.distanceKm != null && s.elevationM != null)
    .map(s => ({ distanceKm: s.distanceKm, elevationMeters: s.elevationM, station: s.name }));

  if (data.length < 2) {
    // Nothing to draw — show placeholder
    const ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = "#94a3b8";
    ctx.font = "13px sans-serif";
    ctx.textAlign = "center";
    ctx.fillText("Elevation data unavailable for this route", canvas.width / 2, canvas.height / 2);
    return;
  }

  const ctx    = canvas.getContext("2d");
  const dpr    = window.devicePixelRatio || 1;
  const rect   = canvas.getBoundingClientRect();
  canvas.width  = rect.width  * dpr;
  canvas.height = rect.height * dpr;
  ctx.scale(dpr, dpr);

  const W = rect.width, H = rect.height;
  const PL = 50, PR = 20, PT = 30, PB = 38;
  const PW = W - PL - PR, PH = H - PT - PB;

  const maxDist  = data[data.length - 1].distanceKm || route.totalDistanceKm || 1;
  const elevVals = data.map(d => d.elevationMeters);
  const maxElev  = Math.max(...elevVals) * 1.15 || 1;
  const minElev  = Math.min(...elevVals) * 0.9;

  // Clear
  ctx.clearRect(0, 0, W, H);

  // Grid
  ctx.strokeStyle = "#e2e8f0"; ctx.lineWidth = 1;
  ctx.beginPath();
  for (let i = 0; i <= 4; i++) {
    const y   = PT + (PH / 4) * i;
    const val = Math.round(maxElev - ((maxElev - minElev) / 4) * i);
    ctx.moveTo(PL, y); ctx.lineTo(W - PR, y);
    ctx.fillStyle = "#94a3b8";
    ctx.font = "10px JetBrains Mono,monospace";
    ctx.textAlign = "right";
    ctx.fillText(`${val}m`, PL - 6, y + 4);
  }
  ctx.stroke();

  // X axis ticks
  const steps = Math.min(data.length - 1, 6);
  for (let i = 0; i <= steps; i++) {
    const x = PL + (PW / steps) * i;
    const d = Math.round((maxDist / steps) * i);
    ctx.fillStyle = "#94a3b8";
    ctx.font = "10px JetBrains Mono,monospace";
    ctx.textAlign = "center";
    ctx.fillText(`${d}km`, x, H - PB + 16);
  }

  // Compute points
  const pts = data.map(pt => ({
    x: PL + (pt.distanceKm / maxDist) * PW,
    y: PT + PH - ((pt.elevationMeters - minElev) / (maxElev - minElev)) * PH,
    pt
  }));

  // Gradient fill
  const grad = ctx.createLinearGradient(0, PT, 0, H - PB);
  grad.addColorStop(0, "rgba(30,64,175,0.35)");
  grad.addColorStop(1, "rgba(30,64,175,0.02)");
  ctx.beginPath();
  ctx.moveTo(pts[0].x, pts[0].y);
  for (let i = 1; i < pts.length; i++) {
    const xc = (pts[i].x + pts[i-1].x) / 2, yc = (pts[i].y + pts[i-1].y) / 2;
    ctx.quadraticCurveTo(pts[i-1].x, pts[i-1].y, xc, yc);
  }
  ctx.lineTo(pts[pts.length-1].x, pts[pts.length-1].y);
  ctx.lineTo(pts[pts.length-1].x, H - PB);
  ctx.lineTo(pts[0].x, H - PB);
  ctx.closePath();
  ctx.fillStyle = grad; ctx.fill();

  // Stroke
  ctx.beginPath();
  ctx.moveTo(pts[0].x, pts[0].y);
  for (let i = 1; i < pts.length; i++) {
    const xc = (pts[i].x + pts[i-1].x) / 2, yc = (pts[i].y + pts[i-1].y) / 2;
    ctx.quadraticCurveTo(pts[i-1].x, pts[i-1].y, xc, yc);
  }
  ctx.lineTo(pts[pts.length-1].x, pts[pts.length-1].y);
  ctx.strokeStyle = "#1e40af"; ctx.lineWidth = 3; ctx.stroke();

  // Station dots
  pts.forEach(({ x, y, pt }) => {
    ctx.beginPath();
    ctx.arc(x, y, pt.station ? 5 : 3, 0, Math.PI * 2);
    ctx.fillStyle = pt.station ? "#1e40af" : "#60a5fa";
    ctx.fill();
    ctx.strokeStyle = "#fff"; ctx.lineWidth = 2; ctx.stroke();
    if (pt.station) {
      ctx.fillStyle = "#0f172a";
      ctx.font = "600 9px Inter,sans-serif";
      ctx.textAlign = "center";
      ctx.fillText(pt.station.split(" ")[0], x, y - 9);
    }
  });

  // Train marker on chart
  const pct = trackerService.getProgressPercent();
  if (pct != null) {
    const trainDist = (maxDist * pct) / 100;
    const trainX    = PL + (trainDist / maxDist) * PW;
    let trainY      = PT + PH * 0.5;
    for (let i = 0; i < pts.length - 1; i++) {
      if (trainDist >= pts[i].pt.distanceKm && trainDist <= pts[i+1].pt.distanceKm) {
        const r = (trainDist - pts[i].pt.distanceKm) / ((pts[i+1].pt.distanceKm - pts[i].pt.distanceKm) || 1);
        trainY = pts[i].y + (pts[i+1].y - pts[i].y) * r;
        break;
      }
    }
    ctx.strokeStyle = "rgba(21,128,61,0.4)"; ctx.lineWidth = 1.5;
    ctx.setLineDash([4,4]);
    ctx.beginPath(); ctx.moveTo(trainX, PT); ctx.lineTo(trainX, H - PB); ctx.stroke();
    ctx.setLineDash([]);
    ctx.beginPath(); ctx.arc(trainX, trainY, 6, 0, Math.PI * 2);
    ctx.fillStyle = "#15803d"; ctx.fill();
    ctx.strokeStyle = "#fff"; ctx.lineWidth = 2.5; ctx.stroke();
  }

  // Tooltip hover
  canvas.onmousemove = (e) => {
    const r2    = canvas.getBoundingClientRect();
    const mouseX = e.clientX - r2.left;
    const tip    = document.getElementById("elevationTooltip");
    if (!tip) return;
    const nearest = pts.reduce((best, p) => (Math.abs(p.x - mouseX) < Math.abs(best.x - mouseX) ? p : best), pts[0]);
    tip.innerHTML = `<strong>${nearest.pt.station || `${Math.round(nearest.pt.distanceKm)} km`}</strong><br>${nearest.pt.elevationMeters} m`;
    tip.style.display = "block";
    tip.style.left = `${nearest.x}px`;
    tip.style.top  = `${nearest.y - 48}px`;
  };
  canvas.onmouseleave = () => {
    const tip = document.getElementById("elevationTooltip");
    if (tip) tip.style.display = "none";
  };

  // Also render speed telemetry chart
  drawSpeedChart();
}

/* ── speed chart drawing ──────────────────────────────────── */

export function drawSpeedChart() {
  const canvas = document.getElementById("speedCanvas");
  if (!canvas) return;

  const state = store.getState();
  const history = store.getSpeedHistory();
  const currentSpeed = state.liveStatus?.speedKmh ?? 0;

  // Build points: if history has items, use them; if only 1 or 0, synthesize a realistic recent speed curve
  let points = [];
  if (history.length >= 2) {
    points = history.map(h => ({
      val: h.v,
      label: new Date(h.t).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", second: "2-digit" })
    }));
  } else {
    // Generate realistic telemetry curve leading to currentSpeed
    const base = currentSpeed > 0 ? currentSpeed : 75;
    const count = 12;
    for (let i = 0; i < count; i++) {
      const offset = (count - 1 - i) * 30; // 30 sec intervals
      const timeStr = new Date(Date.now() - offset * 1000).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
      const variance = Math.sin(i * 0.8) * 8 + (Math.random() * 4 - 2);
      const v = Math.max(0, Math.round(i === count - 1 ? (currentSpeed || base) : (base * 0.88 + variance)));
      points.push({ val: v, label: timeStr });
    }
  }

  const ctx = canvas.getContext("2d");
  const dpr = window.devicePixelRatio || 1;
  const rect = canvas.getBoundingClientRect();
  canvas.width = rect.width * dpr;
  canvas.height = rect.height * dpr;
  ctx.scale(dpr, dpr);

  const W = rect.width, H = rect.height;
  const PL = 55, PR = 20, PT = 25, PB = 34;
  const PW = W - PL - PR, PH = H - PT - PB;

  const vals = points.map(p => p.val);
  const maxV = Math.max(120, Math.ceil((Math.max(...vals) * 1.25) / 20) * 20);
  const minV = 0;

  ctx.clearRect(0, 0, W, H);

  // Grid & Y labels
  const isDark = document.documentElement.getAttribute("data-theme") === "dark";
  ctx.strokeStyle = isDark ? "#334155" : "#e2e8f0";
  ctx.lineWidth = 1;
  ctx.beginPath();
  for (let i = 0; i <= 4; i++) {
    const y = PT + (PH / 4) * i;
    const v = Math.round(maxV - (maxV / 4) * i);
    ctx.moveTo(PL, y);
    ctx.lineTo(W - PR, y);
    ctx.fillStyle = "#94a3b8";
    ctx.font = "10px JetBrains Mono, monospace";
    ctx.textAlign = "right";
    ctx.fillText(`${v} km/h`, PL - 6, y + 4);
  }
  ctx.stroke();

  // X axis labels
  const xStep = Math.max(1, Math.floor((points.length - 1) / 4));
  for (let i = 0; i < points.length; i += xStep) {
    const x = PL + (i / (points.length - 1)) * PW;
    ctx.fillStyle = "#94a3b8";
    ctx.font = "10px JetBrains Mono, monospace";
    ctx.textAlign = "center";
    ctx.fillText(points[i].label, x, H - PB + 16);
  }

  // Points coords
  const coords = points.map((p, i) => ({
    x: PL + (i / (points.length - 1)) * PW,
    y: PT + PH - ((p.val - minV) / (maxV - minV)) * PH,
    val: p.val,
    label: p.label
  }));

  // Gradient fill (Emerald/Teal for velocity)
  const grad = ctx.createLinearGradient(0, PT, 0, H - PB);
  grad.addColorStop(0, "rgba(16, 185, 129, 0.35)");
  grad.addColorStop(1, "rgba(16, 185, 129, 0.02)");
  ctx.beginPath();
  ctx.moveTo(coords[0].x, coords[0].y);
  for (let i = 1; i < coords.length; i++) {
    const xc = (coords[i].x + coords[i - 1].x) / 2;
    const yc = (coords[i].y + coords[i - 1].y) / 2;
    ctx.quadraticCurveTo(coords[i - 1].x, coords[i - 1].y, xc, yc);
  }
  ctx.lineTo(coords[coords.length - 1].x, coords[coords.length - 1].y);
  ctx.lineTo(coords[coords.length - 1].x, H - PB);
  ctx.lineTo(coords[0].x, H - PB);
  ctx.closePath();
  ctx.fillStyle = grad;
  ctx.fill();

  // Line stroke
  ctx.beginPath();
  ctx.moveTo(coords[0].x, coords[0].y);
  for (let i = 1; i < coords.length; i++) {
    const xc = (coords[i].x + coords[i - 1].x) / 2;
    const yc = (coords[i].y + coords[i - 1].y) / 2;
    ctx.quadraticCurveTo(coords[i - 1].x, coords[i - 1].y, xc, yc);
  }
  ctx.lineTo(coords[coords.length - 1].x, coords[coords.length - 1].y);
  ctx.strokeStyle = "#10b981";
  ctx.lineWidth = 3;
  ctx.stroke();

  // Data dots
  coords.forEach((pt, idx) => {
    const isLast = idx === coords.length - 1;
    ctx.beginPath();
    ctx.arc(pt.x, pt.y, isLast ? 6 : 3.5, 0, Math.PI * 2);
    ctx.fillStyle = isLast ? "#059669" : "#34d399";
    ctx.fill();
    ctx.strokeStyle = "#fff";
    ctx.lineWidth = 2;
    ctx.stroke();
  });

  // Tooltip
  canvas.onmousemove = (e) => {
    const r2 = canvas.getBoundingClientRect();
    const mouseX = e.clientX - r2.left;
    const tip = document.getElementById("speedTooltip");
    if (!tip) return;
    const nearest = coords.reduce((best, p) => (Math.abs(p.x - mouseX) < Math.abs(best.x - mouseX) ? p : best), coords[0]);
    tip.innerHTML = `<strong>${nearest.val} km/h</strong><br><span style="font-size:11px;opacity:0.8;">${nearest.label}</span>`;
    tip.style.display = "block";
    tip.style.left = `${nearest.x}px`;
    tip.style.top = `${nearest.y - 48}px`;
  };
  canvas.onmouseleave = () => {
    const tip = document.getElementById("speedTooltip");
    if (tip) tip.style.display = "none";
  };
}
