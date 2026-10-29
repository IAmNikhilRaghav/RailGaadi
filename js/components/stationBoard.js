/**
 * RailGaadi Station Live Departure Board — Phase 2
 * Shows all arriving/departing trains at the current or destination station
 * Data: Live RailRadar Station API (/stations/{code}/trains) with resilient schedule fallback
 */

import { store } from "../state/store.js";
import { trackerService } from "../services/trackerService.js";
import { fetchStationBoardAPI, POPULAR_TRAINS } from "../services/railradarService.js";

let boardStationCode = null; // current station code being shown
let boardMode        = "departure"; // "departure" | "arrival"
let boardTrains      = null;
let boardFetchingFor = null;
let isLiveBoard      = false;

/* ── helpers ──────────────────────────────────────────────── */

function fmtTime(str) {
  if (!str || str === "—") return "—";
  try {
    if (str.includes("T")) {
      return new Date(str).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" });
    }
    return str;
  } catch { return str; }
}

function statusBadge(delay) {
  if (delay == null) return `<span class="board-badge neutral">Scheduled</span>`;
  if (delay === 0)   return `<span class="board-badge on-time">On Time</span>`;
  if (delay > 0)     return `<span class="board-badge delayed">+${delay}m</span>`;
  return `<span class="board-badge early">${Math.abs(delay)}m early</span>`;
}

/* ── Generate departure board from curated data ─────────────── */

function getBoardTrains(station) {
  if (!station) return [];

  const code = (station.stationId || station.code || "").toUpperCase();
  const name = station.name || "";

  // Find all trains that pass through this station
  const matches = [];
  POPULAR_TRAINS.forEach(train => {
    const idx = (train.stations || []).findIndex(s =>
      (s.stationId || s.code || "").toUpperCase() === code || s.name === name
    );
    if (idx >= 0) {
      const stn = train.stations[idx];
      matches.push({
        trainId:    train.trainId,
        number:     train.number,
        name:       train.name,
        type:       train.type,
        origin:     train.stations[0].name,
        destination: train.stations[train.stations.length - 1].name,
        scheduledArr: stn.scheduledArr !== "—" ? stn.scheduledArr : null,
        scheduledDep: stn.scheduledDep !== "—" ? stn.scheduledDep : null,
        platform:   stn.platform,
        haltMin:    stn.haltMin,
        delayMinutes: null,
        stopIndex:  idx
      });
    }
  });

  return matches.sort((a, b) => {
    const ta = a.scheduledDep || a.scheduledArr || "99:99";
    const tb = b.scheduledDep || b.scheduledArr || "99:99";
    return ta.localeCompare(tb);
  });
}

/* ── empty state ─────────────────────────────────────────────── */

function renderBoardEmpty() {
  return `
    <div class="board-container" style="display:flex;align-items:center;justify-content:center;min-height:400px;">
      <div style="text-align:center;color:var(--color-text-muted);">
        <div style="font-size:52px;margin-bottom:16px;">🚉</div>
        <h3 style="font-size:var(--text-lg);font-weight:700;color:var(--color-text-secondary);margin-bottom:8px;">Station Board Unavailable</h3>
        <p style="font-size:var(--text-sm);max-width:320px;margin:0 auto;">
          Track a train to see departures and arrivals at your current or destination station.
        </p>
      </div>
    </div>
  `;
}

/* ── Asynchronously fetch live station departures ─────────────── */

async function fetchLiveBoardData(stnCode) {
  if (!stnCode || boardFetchingFor === stnCode) return;
  boardFetchingFor = stnCode;

  try {
    const data = await fetchStationBoardAPI(stnCode);
    if (data?.trains && data.trains.length > 0) {
      boardTrains = data.trains;
      isLiveBoard = true;
      const root = document.getElementById("stationBoardRoot");
      if (root) {
        root.innerHTML = renderStationBoard();
        attachStationBoardEvents();
      }
    }
  } catch (err) {
    console.warn("[StationBoard] live fetch error:", err);
  } finally {
    boardFetchingFor = null;
  }
}

/* ── Main render ───────────────────────────────────────────── */

export function renderStationBoard() {
  const state = store.getState();
  const { activeTrainId, liveRoute, liveStatus, activeTrain } = state;

  if (!activeTrainId || !liveRoute) return renderBoardEmpty();

  const stations = liveRoute.stations || [];
  const curIdx   = trackerService.getCurrentStationIndex();
  const curStn   = stations[curIdx] || null;
  const destStn  = stations[stations.length - 1] || null;

  // Choose between current station and destination as the board station
  const displayStation = boardStationCode === (destStn?.stationId || destStn?.code)
    ? destStn
    : (curStn || destStn);

  const targetCode = (displayStation?.stationId || displayStation?.code || "").toUpperCase();

  // Trigger non-blocking live fetch if not already done for this station
  if (targetCode && boardFetchingFor !== targetCode && (!boardTrains || boardStationCode !== targetCode)) {
    boardStationCode = targetCode;
    fetchLiveBoardData(targetCode);
  }

  const trains = boardTrains !== null ? boardTrains : getBoardTrains(displayStation);

  const trainTypeColors = {
    "Rajdhani":    "#ef4444",
    "Shatabdi":    "#f97316",
    "Vande Bharat":"#8b5cf6",
    "Duronto":     "#0ea5e9",
    "Garib Rath":  "#10b981",
    "Express":     "#1e40af",
    "Mail":        "#6b7280",
    "Passenger":   "#64748b",
    "EMU":         "#059669",
    "MEMU":        "#0d9488",
    "Local":       "#059669"
  };

  const filteredTrains = boardMode === "departure"
    ? trains.filter(t => t.scheduledDep)
    : trains.filter(t => t.scheduledArr);

  const timeField = boardMode === "departure" ? "scheduledDep" : "scheduledArr";

  return `
    <div class="board-container" aria-label="Station Departure Board">

      <!-- Board Header -->
      <div class="board-header">
        <div class="board-station-info">
          <div style="display:flex;align-items:center;gap:10px;">
            <div style="width:40px;height:40px;background:linear-gradient(135deg,#1e40af 0%,#3b82f6 100%);border-radius:10px;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/><path d="M12 8v4l2 2"/>
              </svg>
            </div>
            <div>
              <h3 style="font-size:var(--text-xl);font-weight:800;color:var(--color-text-primary);">
                ${displayStation?.name || "Unknown Station"}
                <span style="font-size:var(--text-sm);font-weight:600;color:var(--color-text-muted);font-family:var(--font-mono);">
                  ${targetCode}
                </span>
              </h3>
              <p style="font-size:var(--text-xs);color:var(--color-text-muted);margin-top:2px;">
                ${isLiveBoard ? "🟢 Live Telemetry · " : ""}${filteredTrains.length} trains ${boardMode === "departure" ? "departing" : "arriving"}
              </p>
            </div>
          </div>
        </div>

        <!-- Station Selector -->
        <div class="board-station-selector">
          <button class="board-stn-btn ${boardStationCode !== (destStn?.stationId || destStn?.code) ? "active" : ""}"
            id="boardCurrentStnBtn">
            📍 Current
          </button>
          ${destStn && destStn !== curStn ? `
            <button class="board-stn-btn ${boardStationCode === (destStn?.stationId || destStn?.code) ? "active" : ""}"
              id="boardDestStnBtn" data-stn-id="${destStn.stationId || destStn.code}">
              🏁 Destination
            </button>
          ` : ""}
        </div>
      </div>

      <!-- Mode Toggle: Departures / Arrivals -->
      <div class="board-mode-tabs">
        <button class="board-mode-btn ${boardMode === "departure" ? "active" : ""}" data-mode="departure" id="boardDepartBtn">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M5 12h14"/><path d="m12 5 7 7-7 7"/>
          </svg>
          Departures (${trains.filter(t => t.scheduledDep).length})
        </button>
        <button class="board-mode-btn ${boardMode === "arrival" ? "active" : ""}" data-mode="arrival" id="boardArriveBtn">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5"/><path d="m12 19-7-7 7-7"/>
          </svg>
          Arrivals (${trains.filter(t => t.scheduledArr).length})
        </button>
      </div>

      <!-- Board Table -->
      ${filteredTrains.length === 0 ? `
        <div style="text-align:center;padding:var(--space-12);color:var(--color-text-muted);">
          <div style="font-size:36px;margin-bottom:12px;">🔍</div>
          <p>No ${boardMode === "departure" ? "departures" : "arrivals"} currently listed for ${displayStation?.name || targetCode}</p>
        </div>
      ` : `
        <!-- Desktop table (hidden on mobile via CSS) -->
        <div class="board-table-wrapper">
          <table class="board-table" aria-label="${boardMode === "departure" ? "Departures" : "Arrivals"} from ${displayStation?.name}">
            <thead>
              <tr>
                <th>Train</th>
                <th>${boardMode === "departure" ? "Destination" : "Origin"}</th>
                <th>${boardMode === "departure" ? "Departs" : "Arrives"}</th>
                <th>Platform</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              ${filteredTrains.map(t => {
                const typeColor = Object.entries(trainTypeColors).find(([k]) => t.type?.includes(k))?.[1] || "#1e40af";
                return `
                  <tr class="board-train-row" data-train-id="${t.trainId}">
                    <td>
                      <div style="display:flex;align-items:center;gap:10px;">
                        <div style="width:5px;height:40px;border-radius:3px;background:${typeColor};flex-shrink:0;"></div>
                        <div>
                          <div style="font-weight:700;font-family:var(--font-mono);color:var(--color-text-primary);">${t.number}</div>
                          <div style="font-size:11px;color:var(--color-text-muted);max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${t.name}</div>
                        </div>
                      </div>
                    </td>
                    <td>
                      <div style="font-weight:600;font-size:var(--text-sm);">
                        ${boardMode === "departure" ? t.destination : t.origin}
                      </div>
                    </td>
                    <td>
                      <div style="font-family:var(--font-mono);font-weight:700;font-size:var(--text-md);color:var(--color-text-primary);">
                        ${fmtTime(t[timeField]) || "—"}
                      </div>
                      ${t.haltMin ? `<div style="font-size:11px;color:var(--color-text-muted);">Halt: ${t.haltMin}m</div>` : ""}
                    </td>
                    <td>
                      ${t.platform && t.platform !== "—" ? `
                        <div style="width:36px;height:36px;background:var(--color-accent-subtle);border:2px solid var(--color-accent-border);border-radius:8px;display:flex;align-items:center;justify-content:center;font-weight:800;font-size:var(--text-sm);color:var(--color-accent);">
                          ${t.platform}
                        </div>
                      ` : `<span style="color:var(--color-text-muted);">—</span>`}
                    </td>
                    <td>${statusBadge(t.delayMinutes)}</td>
                    <td>
                      <button class="board-track-btn" data-train-id="${t.trainId}" title="Track this train">
                        Track
                        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M5 12h14"/><path d="m12 5 7 7-7 7"/>
                        </svg>
                      </button>
                    </td>
                  </tr>
                `;
              }).join("")}
            </tbody>
          </table>
        </div>

        <!-- Mobile card list (shown on mobile via CSS, hidden on desktop) -->
        <div class="board-mobile-list" role="list">
          ${filteredTrains.map(t => {
            const typeColor = Object.entries(trainTypeColors).find(([k]) => t.type?.includes(k))?.[1] || "#1e40af";
            return `
              <div class="board-mobile-card board-train-row" data-train-id="${t.trainId}" role="listitem">
                <div style="width:4px;align-self:stretch;border-radius:4px;background:${typeColor};flex-shrink:0;"></div>
                <div class="board-mobile-card-left">
                  <div class="board-mobile-card-name">${t.name}</div>
                  <div class="board-mobile-card-route">
                    ${boardMode === "departure" ? "→ " + t.destination : "← " + t.origin}
                  </div>
                  <div class="board-mobile-card-num">${t.number}</div>
                </div>
                <div class="board-mobile-card-right">
                  <div class="board-mobile-card-time">${fmtTime(t[timeField]) || "—"}</div>
                  ${statusBadge(t.delayMinutes)}
                  ${t.platform && t.platform !== "—" ? `<div style="font-size:11px;font-weight:700;color:var(--color-accent);">Plt ${t.platform}</div>` : ""}
                </div>
              </div>
            `;
          }).join("")}
        </div>
      `}

      <div style="padding:var(--space-4);font-size:11px;color:var(--color-text-muted);text-align:center;border-top:1px solid var(--color-border);">
        Live station timetable · Station: ${displayStation?.name} (${targetCode})
      </div>
    </div>
  `;
}

/* ── Events ────────────────────────────────────────────────── */

export function attachStationBoardEvents() {
  const state  = store.getState();
  const route  = state.liveRoute;
  const curIdx = trackerService.getCurrentStationIndex();
  const curStn = route?.stations?.[curIdx] || null;
  const destStn = route?.stations?.[route.stations.length - 1] || null;

  // Mode toggle
  document.getElementById("boardDepartBtn")?.addEventListener("click", () => {
    boardMode = "departure";
    refreshBoard();
  });
  document.getElementById("boardArriveBtn")?.addEventListener("click", () => {
    boardMode = "arrival";
    refreshBoard();
  });

  // Station selector
  document.getElementById("boardCurrentStnBtn")?.addEventListener("click", () => {
    const code = (curStn?.stationId || curStn?.code || "").toUpperCase();
    boardStationCode = code;
    boardTrains = null;
    fetchLiveBoardData(code);
    refreshBoard();
  });
  document.getElementById("boardDestStnBtn")?.addEventListener("click", () => {
    const code = (destStn?.stationId || destStn?.code || "").toUpperCase();
    boardStationCode = code;
    boardTrains = null;
    fetchLiveBoardData(code);
    refreshBoard();
  });

  // Track buttons in board rows
  document.querySelectorAll(".board-track-btn, .board-train-row").forEach(el => {
    el.addEventListener("click", (e) => {
      const row = e.target.closest("[data-train-id]") || el;
      const trainId = row.getAttribute("data-train-id");
      if (trainId) {
        trackerService.loadTrain(trainId);
        store.setActiveTab("tracking");
      }
    });
  });
}

function refreshBoard() {
  const root = document.getElementById("stationBoardRoot");
  if (root) {
    root.innerHTML = renderStationBoard();
    attachStationBoardEvents();
  }
}
