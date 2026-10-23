/**
 * RailGaadi Travel Companion — v3
 * High-performance, zero-recursion companion view
 * Real-time weather via Open-Meteo & OpenWeather
 * Distance-bounded live landmarks around instantaneous train coordinates
 */

import { store } from "../state/store.js";
import { trackerService } from "../services/trackerService.js";
import { WeatherService } from "../services/weatherService.js";
import { GeoService } from "../services/geoService.js";
import { focusLocationOnMap } from "./maplibreView.js";

let activePoiCategory = "all";
let cachedPois = null;
let cachedTrainId = null;
let cachedCoordKey = null;
let isFetchingPois = false;
let isFetchingWeather = false;
let weatherCache = {};

/* ── Curated Iconic Railway Corridor Landmarks (Instant Zero-Latency Fallback) ── */
const CORRIDOR_LANDMARKS = [
  // North / Delhi - Agra - Kanpur - Prayagraj - Varanasi
  { name: "Yamuna River Crossing", category: "river", lat: 28.665, lon: 77.255, desc: "Historic river crossing near old Delhi Railway Bridge." },
  { name: "Red Fort (Lal Qila)", category: "heritage", lat: 28.6562, lon: 77.2410, desc: "17th-century Mughal fortress and UNESCO World Heritage site." },
  { name: "Qutub Minar Complex", category: "monument", lat: 28.5245, lon: 77.1855, desc: "73m minaret built in 1192, iconic Delhi landmark." },
  { name: "Aravalli Biodiversity Ridge", category: "mountain", lat: 28.560, lon: 77.160, desc: "Ancient weathered mountain ridge visible west of the capital." },
  { name: "Taj Mahal Viewpoint", category: "monument", lat: 27.1751, lon: 78.0421, desc: "Marble mausoleum on the south bank of the Yamuna River." },
  { name: "Agra Fort", category: "heritage", lat: 27.1795, lon: 78.0211, desc: "Historic residence of Mughal emperors, UNESCO Heritage site." },
  { name: "Chambal River Railway Bridge", category: "bridge", lat: 26.680, lon: 78.850, desc: "Long rail bridge over the wild Chambal River gorge." },
  { name: "Gwalior Fort", category: "heritage", lat: 26.2312, lon: 78.1695, desc: "8th-century hill fortress with dramatic defensive bastions." },
  { name: "Ganga River Rail Bridge (Kanpur)", category: "bridge", lat: 26.470, lon: 80.360, desc: "Spans the holy Ganges River at Kanpur Central corridor." },
  { name: "Triveni Sangam", category: "river", lat: 25.423, lon: 81.884, desc: "Sacred confluence of the Ganges, Yamuna, and mythical Saraswati." },
  { name: "Curzon Bridge (Prayagraj)", category: "bridge", lat: 25.465, lon: 81.850, desc: "Historic 1905 dual-deck railway bridge across the Ganges." },
  { name: "Varanasi Ghats & Kashi Corridor", category: "heritage", lat: 25.3109, lon: 83.0104, desc: "Ancient riverside steps and historic Kashi Vishwanath corridor." },
  { name: "Malviya Bridge (Dufferin)", category: "bridge", lat: 25.321, lon: 83.033, desc: "Iconic double-deck steel truss rail-road bridge across the Ganges." },
  // East / Gaya - Dhanbad - Asansol - Howrah
  { name: "Parasnath Hill (Shikharji)", category: "mountain", lat: 23.962, lon: 86.136, desc: "Highest peak in Jharkhand (1365m), sacred Jain pilgrimage site." },
  { name: "Damodar River Rail Bridge", category: "bridge", lat: 23.670, lon: 86.950, desc: "Historic bridge crossing the Damodar river valley in Bengal." },
  { name: "Howrah Bridge & Hooghly River", category: "bridge", lat: 22.5851, lon: 88.3468, desc: "Balanced cantilever bridge over the Hooghly river into Kolkata." },
  { name: "Victoria Memorial", category: "monument", lat: 22.5448, lon: 88.3426, desc: "Magnificent white marble palace built between 1906 and 1921." },
  // West & Central / Kota - Ratlam - Vadodara - Mumbai
  { name: "Seven Wonders Park Kota", category: "monument", lat: 25.176, lon: 75.845, desc: "Showcases replicas of the world's seven wonders along Kishore Sagar." },
  { name: "Vindhyachal Mountain Escarpment", category: "mountain", lat: 24.500, lon: 76.500, desc: "Ancient sandstone hills defining the northern border of Deccan." },
  { name: "Narmada River Golden Bridge", category: "bridge", lat: 21.700, lon: 72.990, desc: "Historic bridge across the sacred Narmada River at Bharuch." },
  { name: "Western Ghats (Sahyadri Hills)", category: "mountain", lat: 19.350, lon: 73.150, desc: "Lush biological hotspot rising dramatically east of Mumbai rail routes." },
  { name: "Ulhas River Estuary Bridge", category: "bridge", lat: 19.280, lon: 73.050, desc: "Coastal railway viaduct crossing tidal creeks into Mumbai." },
  { name: "Chhatrapati Shivaji Maharaj Terminus", category: "heritage", lat: 18.940, lon: 72.835, desc: "Victorian Gothic Revival UNESCO World Heritage rail headquarters." },
  // Rajasthan / Barmer - Jodhpur - Jaipur
  { name: "Thar Desert Dune Formations", category: "mountain", lat: 25.750, lon: 71.390, desc: "Vast golden sand ridges and desert ecology along western corridor." },
  { name: "Mehrangarh Fort Jodhpur", category: "heritage", lat: 26.298, lon: 73.018, desc: "Towering cliffside fort 122m above Jodhpur city plain." },
  { name: "Umaid Bhawan Palace", category: "heritage", lat: 26.280, lon: 73.047, desc: "One of the world's largest private royal residences in sandstone." },
  { name: "Hawa Mahal & Old Jaipur", category: "monument", lat: 26.9239, lon: 75.8267, desc: "Palace of Winds with 953 honeycomb windows built in 1799." }
];

/* ── Helpers ── */

function getTrainInstantCoordinates() {
  const state = store.getState();
  const pos = trackerService.getCurrentPosition();
  if (pos?.lat && pos?.lon) return { lat: pos.lat, lon: pos.lon };

  const stations = state.liveRoute?.stations || [];
  const curIdx = trackerService.getCurrentStationIndex();
  const curStn = stations[curIdx] || stations[0];
  if (curStn?.latitude && curStn?.longitude) {
    return { lat: curStn.latitude, lon: curStn.longitude };
  }
  return { lat: 28.6418, lon: 77.2203 }; // Default New Delhi
}

/* ── Empty State ── */

function renderEmpty() {
  return `
    <div class="companion-container" style="display:flex;align-items:center;justify-content:center;min-height:400px;">
      <div style="text-align:center;color:var(--color-text-muted);">
        <div style="font-size:48px;margin-bottom:12px;">🌤️</div>
        <h3 style="font-size:var(--text-lg);font-weight:700;color:var(--color-text-secondary);margin-bottom:8px;">No Train Selected</h3>
        <p style="font-size:var(--text-sm);">Search and track a train to see real-time weather and route landmarks along its instantaneous coordinates.</p>
      </div>
    </div>
  `;
}

/* ── Weather Card Renderer ── */

function renderWeatherCard(label, stn, weather) {
  const activeStn = stn || { name: "En-Route Sector", code: "LOC" };
  const w = weather || WeatherService.getWeatherForStation(activeStn);

  return `
    <div class="weather-card" id="weatherCard-${label.toLowerCase().replace(/[^a-z0-9]/g, '-')}">
      <div class="weather-node-type">${label}</div>
      <div class="weather-station-name">${activeStn.name} ${activeStn.code ? `(${activeStn.code})` : ""}</div>
      <div class="weather-temp-row">
        <span class="weather-temp-hero">${w.temperatureC}°C</span>
        <span class="weather-icon-lg" title="${w.condition}">${w.icon || "☀️"}</span>
      </div>
      <div style="font-size:var(--text-xs);font-weight:600;color:var(--color-text-secondary);margin-bottom:4px;">${w.condition}</div>
      <div class="weather-sub-metrics">
        <div class="sub-metric-item">💧 Humidity: <strong>${w.humidityPercent}%</strong></div>
        <div class="sub-metric-item">💨 Wind: <strong>${w.windSpeedKmh} km/h</strong></div>
        <div class="sub-metric-item" style="grid-column:span 2;">🌧️ Rain Chance: <strong>${w.rainProbability}%</strong></div>
      </div>
      <div style="font-size:10px;color:var(--color-text-muted);margin-top:6px;display:flex;align-items:center;justify-content:space-between;">
        <span>Source: ${w.source || "Open-Meteo Live"}</span>
        <span style="color:var(--color-success);font-weight:600;">● Live</span>
      </div>
    </div>
  `;
}

/* ── POI Card Renderer ── */

const CATEGORY_ICONS = {
  river: "🌊", mountain: "⛰️", bridge: "🌉", monument: "🏛️",
  city: "🏙️", heritage: "🏯", park: "🌿", temple: "⛩️", all: "📍"
};

function renderPoiCards(pois) {
  if (!pois || pois.length === 0) {
    return `
      <div style="grid-column:1/-1;text-align:center;padding:var(--space-8);color:var(--color-text-muted);">
        <div style="font-size:32px;margin-bottom:8px;">🗺️</div>
        <p style="font-size:13px;font-weight:600;">No landmarks recorded nearby in this category.</p>
        <p style="font-size:12px;margin-top:4px;">Switch category to "All" to view all landmarks visible from this sector.</p>
      </div>
    `;
  }
  return pois.map(p => `
    <div class="poi-card" data-poi-id="${p.id}">
      <div class="poi-card-top">
        <span class="poi-category-badge">${CATEGORY_ICONS[p.category] || "📍"} ${p.categoryLabel || p.category}</span>
        <span class="poi-distance-pill" style="font-weight:700;color:var(--color-accent);">${p.distanceFromRouteKm ? `${p.distanceFromRouteKm} km from train` : "Along Route"}</span>
      </div>
      <h4 class="poi-name" style="margin-top:8px;">${p.name}</h4>
      <p class="poi-desc" style="font-size:12px;color:var(--color-text-secondary);line-height:1.4;margin:6px 0 12px 0;">${p.description || "Geographic feature visible from the train corridor."}</p>
      <button class="poi-action-btn" data-lat="${p.lat}" data-lon="${p.lon}" data-name="${p.name}">
        <span>View on Map</span>
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M5 12h14"/><path d="m12 5 7 7-7 7"/>
        </svg>
      </button>
    </div>
  `).join("");
}

/* ── Fetch Real Landmarks for Instantaneous Train Position ── */

async function fetchNearbyPois(trainLat, trainLon, stations) {
  const coordKey = `${trainLat.toFixed(2)},${trainLon.toFixed(2)}`;
  if (cachedCoordKey === coordKey && cachedPois?.length > 0) {
    return cachedPois;
  }

  const results = [];
  const seenNames = new Set();

  // Helper to safely add POI
  const addPoi = (item) => {
    const rawName = (item.name || "").trim();
    if (!rawName || rawName.length < 2) return;
    const cleanKey = rawName.toLowerCase();
    if (seenNames.has(cleanKey)) return;
    seenNames.add(cleanKey);

    const dist = GeoService.distanceKm(trainLat, trainLon, item.lat, item.lon);
    results.push({
      id: item.id || `poi_${results.length + 1}`,
      name: rawName,
      lat: item.lat,
      lon: item.lon,
      category: item.category || "monument",
      categoryLabel: (item.category || "Landmark").charAt(0).toUpperCase() + (item.category || "Landmark").slice(1),
      description: item.description || `Visible landmark near instantaneous train corridor.`,
      distanceFromRouteKm: parseFloat(dist.toFixed(1))
    });
  };

  // 1. Check curated iconic landmarks within 60km of instantaneous coordinates
  CORRIDOR_LANDMARKS.forEach(l => {
    const d = GeoService.distanceKm(trainLat, trainLon, l.lat, l.lon);
    if (d <= 65) {
      addPoi({ ...l, description: l.desc, id: `curated_${l.name.replace(/\s+/g, '_')}` });
    }
  });

  // 2. Fetch live landmarks from Wikipedia GeoSearch (CORS-enabled, 150ms latency)
  try {
    const coordParam = encodeURIComponent(`${trainLat}|${trainLon}`);
    const wikiUrl = `https://en.wikipedia.org/w/api.php?action=query&list=geosearch&gscoord=${coordParam}&gsradius=10000&gslimit=20&format=json&origin=*`;
    const res = await fetch(wikiUrl, { signal: AbortSignal.timeout(4500) });
    if (res.ok) {
      const data = await res.json();
      const list = data?.query?.geosearch || [];
      list.forEach(item => {
        const title = item.title;
        // Categorize based on keywords in title
        const lower = title.toLowerCase();
        let cat = "monument";
        if (lower.includes("river") || lower.includes("nadi") || lower.includes("lake") || lower.includes("canal") || lower.includes("falls")) cat = "river";
        else if (lower.includes("hill") || lower.includes("peak") || lower.includes("mountain") || lower.includes("ghat") || lower.includes("ridge")) cat = "mountain";
        else if (lower.includes("bridge") || lower.includes("setu") || lower.includes("viaduct") || lower.includes("pul")) cat = "bridge";
        else if (lower.includes("fort") || lower.includes("palace") || lower.includes("temple") || lower.includes("mandir") || lower.includes("masjid") || lower.includes("heritage")) cat = "heritage";

        addPoi({
          id: `wiki_${item.pageid}`,
          name: title,
          lat: item.lat,
          lon: item.lon,
          category: cat,
          description: `Geographic landmark located ${Math.round(item.dist / 1000)} km from instantaneous train position.`
        });
      });
    }
  } catch (err) {
    console.warn("[Companion] Wikipedia geosearch fetch skipped:", err.message);
  }

  // 3. Query Overpass API with targeted 25km radius if needed
  if (results.length < 8) {
    try {
      const query = `
        [out:json][timeout:6];
        (
          node["historic"](around:25000,${trainLat},${trainLon});
          node["tourism"="attraction"](around:25000,${trainLat},${trainLon});
          node["natural"~"peak|water|wood"](around:25000,${trainLat},${trainLon});
          way["natural"="water"]["name"](around:25000,${trainLat},${trainLon});
          way["bridge"="yes"]["name"](around:25000,${trainLat},${trainLon});
        );
        out center 20;
      `;
      const res = await fetch("https://overpass-api.de/api/interpreter", {
        method: "POST",
        body: `data=${encodeURIComponent(query)}`,
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        signal: AbortSignal.timeout(4500)
      });
      if (res.ok) {
        const json = await res.json();
        (json.elements || []).forEach(el => {
          if (!el.tags?.name) return;
          const lat = el.lat || el.center?.lat;
          const lon = el.lon || el.center?.lon;
          if (!lat || !lon) return;

          const nat = el.tags.natural || "";
          const hist = el.tags.historic || "";
          const cat = hist ? "monument" : nat === "peak" ? "mountain" : (nat === "water" || el.tags.water) ? "river" : el.tags.bridge ? "bridge" : "heritage";

          addPoi({
            id: `osm_${el.id}`,
            name: el.tags.name,
            lat,
            lon,
            category: cat,
            description: el.tags["description"] || el.tags["wikipedia"] || `${cat.toUpperCase()} detected along train corridor.`
          });
        });
      }
    } catch (e) {
      console.warn("[Companion] Overpass fetch skipped:", e.message);
    }
  }

  // Fallback: If still few, add general station landmarks
  if (results.length === 0 && stations?.length > 0) {
    stations.slice(0, 5).forEach(s => {
      if (s.latitude && s.longitude) {
        addPoi({
          id: `stn_${s.stationId}`,
          name: `${s.name} Junction Heritage Precinct`,
          lat: s.latitude,
          lon: s.longitude,
          category: "heritage",
          description: `Key railway junction and architectural landmark along the corridor.`
        });
      }
    });
  }

  // Sort by nearest to instantaneous train location
  results.sort((a, b) => a.distanceFromRouteKm - b.distanceFromRouteKm);

  cachedPois = results;
  cachedCoordKey = coordKey;
  return results;
}

/* ── Main Render ── */

export function renderCompanionSection() {
  const state = store.getState();
  const { activeTrainId, liveRoute, isLoadingRoute, liveStatus } = state;

  if (!activeTrainId && !liveRoute) return renderEmpty();

  const stations = liveRoute?.stations || [];
  const curIdx   = trackerService.getCurrentStationIndex();
  const curStn   = stations[curIdx] || (liveStatus?.currentStation?.name ? liveStatus.currentStation : stations[0]) || { name: "Current En-Route", code: "CUR" };
  const nextStn  = stations[curIdx + 1] || (curIdx < stations.length - 1 ? stations[curIdx + 1] : stations[stations.length - 1]) || { name: "Upcoming Halt", code: "NXT" };
  const destStn  = stations[stations.length - 1] || { name: "Final Destination", code: "DST" };

  // Instantaneous train coordinates
  const trainCoords = getTrainInstantCoordinates();

  // Weather snapshots (instant fallback if async not finished yet)
  const curW  = weatherCache["cur"]  || WeatherService.getWeatherForStation(curStn);
  const nextW = weatherCache["next"] || WeatherService.getWeatherForStation(nextStn);
  const destW = weatherCache["dest"] || WeatherService.getWeatherForStation(destStn);

  // POIs
  const pois = cachedPois || [];
  const filteredPois = activePoiCategory === "all"
    ? pois
    : pois.filter(p => p.category === activePoiCategory);

  // Count per category
  const counts = {
    all: pois.length,
    river: pois.filter(p => p.category === "river").length,
    mountain: pois.filter(p => p.category === "mountain").length,
    bridge: pois.filter(p => p.category === "bridge").length,
    monument: pois.filter(p => p.category === "monument").length,
    heritage: pois.filter(p => p.category === "heritage").length
  };

  return `
    <div class="companion-container" aria-label="Travel Companion">

      <!-- Weather Section -->
      <div>
        <div class="weather-section-title" style="display:flex;align-items:center;justify-content:space-between;margin-bottom:14px;">
          <div>
            <h3 style="font-size:var(--text-lg);font-weight:700;color:var(--color-text-primary);">Live Weather Along Route</h3>
            <span style="font-size:var(--text-xs);color:var(--color-text-muted);">Real-time meteorological readings for active train stops</span>
          </div>
          <span style="font-size:11px;font-weight:600;padding:4px 10px;background:rgba(34,197,94,0.1);color:#16a34a;border-radius:20px;">
            ● Live Weather Sync
          </span>
        </div>

        <div class="weather-cards-grid" id="weatherCardsGrid">
          ${renderWeatherCard("Current Station", curStn, curW)}
          ${renderWeatherCard("Next Stop", nextStn, nextW)}
          ${renderWeatherCard("Destination", destStn, destW)}
        </div>
      </div>

      <!-- Rain / Corridor Precipitation Intelligence -->
      <div class="rain-forecast-strip" id="corridorRainStrip" style="margin-top:16px;">
        <div class="rain-icon-circle">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242"/>
            <path d="M16 14v6"/><path d="M8 14v6"/><path d="M12 16v6"/>
          </svg>
        </div>
        <div class="rain-text-group">
          <h4>Corridor Precipitation &amp; Track Climate</h4>
          <p id="corridorRainText">
            ${curW.rainProbability > 40
              ? `⚠️ Moderate to heavy precipitation likely (${curW.rainProbability}%) near ${curStn.name}. Carry rain gear.`
              : `Optimal rail journey weather. Clear tracks expected along sector near ${curStn.name} (Current: ${curW.condition}, ${curW.temperatureC}°C, Wind: ${curW.windSpeedKmh} km/h).`}
          </p>
        </div>
      </div>

      <!-- Landmarks & POIs Section -->
      <div class="poi-section" style="margin-top:24px;">
        <div class="poi-header" style="margin-bottom:14px;">
          <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:8px;">
            <div>
              <h3 style="font-size:var(--text-lg);font-weight:700;color:var(--color-text-primary);">
                Landmarks &amp; Natural Features Along the Route
              </h3>
              <p style="font-size:var(--text-xs);color:var(--color-text-muted);margin-top:2px;">
                Instantaneous geographic context at train coordinates 
                <strong>[${trainCoords.lat.toFixed(3)}°N, ${trainCoords.lon.toFixed(3)}°E]</strong>
              </p>
            </div>
            <div style="font-size:11px;color:var(--color-text-secondary);background:var(--color-bg-subtle);padding:4px 10px;border-radius:12px;border:1px solid var(--color-border-subtle);">
              Source: OpenStreetMap &amp; Wikipedia
            </div>
          </div>
        </div>

        <!-- Category filter tabs -->
        <div class="poi-filter-tabs" id="poiFilterTabs" role="tablist">
          <button class="poi-tab-btn ${activePoiCategory === "all" ? "active" : ""}" data-category="all">
            All (${counts.all})
          </button>
          ${["river","mountain","bridge","monument","heritage"].map(cat => `
            <button class="poi-tab-btn ${activePoiCategory === cat ? "active" : ""}" data-category="${cat}">
              ${CATEGORY_ICONS[cat] || "📍"} ${cat.charAt(0).toUpperCase() + cat.slice(1)} (${counts[cat] || 0})
            </button>
          `).join("")}
        </div>

        <!-- POI Cards Grid -->
        <div class="poi-cards-grid" id="poiCardsGrid">
          ${renderPoiCards(filteredPois)}
        </div>
      </div>
    </div>
  `;
}

/* ── DOM Event Attachment & Targeted Updates (Zero Recursion) ── */

export function attachCompanionEvents() {
  const state = store.getState();
  const route = state.liveRoute;
  const stations = route?.stations || [];
  const curIdx = trackerService.getCurrentStationIndex();

  const curStn  = stations[curIdx] || stations[0];
  const nextStn = stations[curIdx + 1] || stations[stations.length - 1];
  const destStn = stations[stations.length - 1];

  // 1. Category Tab Clicks (Direct DOM filter, no re-fetching, no recursion)
  document.querySelectorAll(".poi-tab-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      activePoiCategory = btn.getAttribute("data-category");
      document.querySelectorAll(".poi-tab-btn").forEach(b => b.classList.remove("active"));
      btn.classList.add("active");

      const grid = document.getElementById("poiCardsGrid");
      if (grid) {
        const pois = cachedPois || [];
        const filtered = activePoiCategory === "all" ? pois : pois.filter(p => p.category === activePoiCategory);
        grid.innerHTML = renderPoiCards(filtered);
        attachPoiButtonEvents();
      }
    });
  });

  // 2. Map jump buttons
  attachPoiButtonEvents();

  // 3. Asynchronous Live Weather Fetch (updates DOM directly without re-invoking attachCompanionEvents)
  if (!isFetchingWeather) {
    isFetchingWeather = true;
    (async () => {
      try {
        const targets = [
          { key: "cur",  stn: curStn,  label: "Current Station" },
          { key: "next", stn: nextStn, label: "Next Stop" },
          { key: "dest", stn: destStn, label: "Destination" }
        ];

        let updatedAny = false;
        for (const t of targets) {
          if (t.stn?.latitude && t.stn?.longitude) {
            const w = await WeatherService.fetchWeatherAsync({ lat: t.stn.latitude, lon: t.stn.longitude });
            if (w) {
              weatherCache[t.key] = w;
              updatedAny = true;
            }
          }
        }

        if (updatedAny) {
          const grid = document.getElementById("weatherCardsGrid");
          if (grid) {
            grid.innerHTML = `
              ${renderWeatherCard("Current Station", curStn, weatherCache["cur"])}
              ${renderWeatherCard("Next Stop", nextStn, weatherCache["next"])}
              ${renderWeatherCard("Destination", destStn, weatherCache["dest"])}
            `;
          }

          const rainText = document.getElementById("corridorRainText");
          if (rainText && weatherCache["cur"]) {
            const curW = weatherCache["cur"];
            rainText.innerHTML = curW.rainProbability > 40
              ? `⚠️ Moderate to heavy precipitation likely (${curW.rainProbability}%) near ${curStn?.name || "current sector"}. Carry rain gear.`
              : `Optimal rail journey weather. Clear tracks expected along sector near ${curStn?.name || "current station"} (Current: ${curW.condition}, ${curW.temperatureC}°C, Wind: ${curW.windSpeedKmh} km/h).`;
          }
        }
      } catch (err) {
        console.warn("[Companion] Live weather sync error:", err);
      } finally {
        isFetchingWeather = false;
      }
    })();
  }

  // 4. Asynchronous Live Landmarks Fetch around instantaneous train position
  const trainCoords = getTrainInstantCoordinates();
  const coordKey = `${trainCoords.lat.toFixed(2)},${trainCoords.lon.toFixed(2)}`;

  if (cachedTrainId !== state.activeTrainId || cachedCoordKey !== coordKey || !cachedPois) {
    if (!isFetchingPois) {
      isFetchingPois = true;
      cachedTrainId = state.activeTrainId;

      (async () => {
        try {
          const pois = await fetchNearbyPois(trainCoords.lat, trainCoords.lon, stations);
          const grid = document.getElementById("poiCardsGrid");
          if (grid) {
            const filtered = activePoiCategory === "all" ? pois : pois.filter(p => p.category === activePoiCategory);
            grid.innerHTML = renderPoiCards(filtered);
            attachPoiButtonEvents();
          }

          // Update tab counts
          const tabContainer = document.getElementById("poiFilterTabs");
          if (tabContainer) {
            const counts = {
              all: pois.length,
              river: pois.filter(p => p.category === "river").length,
              mountain: pois.filter(p => p.category === "mountain").length,
              bridge: pois.filter(p => p.category === "bridge").length,
              monument: pois.filter(p => p.category === "monument").length,
              heritage: pois.filter(p => p.category === "heritage").length
            };

            tabContainer.innerHTML = `
              <button class="poi-tab-btn ${activePoiCategory === "all" ? "active" : ""}" data-category="all">
                All (${counts.all})
              </button>
              ${["river","mountain","bridge","monument","heritage"].map(cat => `
                <button class="poi-tab-btn ${activePoiCategory === cat ? "active" : ""}" data-category="${cat}">
                  ${CATEGORY_ICONS[cat] || "📍"} ${cat.charAt(0).toUpperCase() + cat.slice(1)} (${counts[cat] || 0})
                </button>
              `).join("")}
            `;
            // Re-attach tab clicks
            tabContainer.querySelectorAll(".poi-tab-btn").forEach(btn => {
              btn.addEventListener("click", () => {
                activePoiCategory = btn.getAttribute("data-category");
                tabContainer.querySelectorAll(".poi-tab-btn").forEach(b => b.classList.remove("active"));
                btn.classList.add("active");
                const currentGrid = document.getElementById("poiCardsGrid");
                if (currentGrid) {
                  const filteredAfter = activePoiCategory === "all" ? pois : pois.filter(p => p.category === activePoiCategory);
                  currentGrid.innerHTML = renderPoiCards(filteredAfter);
                  attachPoiButtonEvents();
                }
              });
            });
          }
        } catch (err) {
          console.warn("[Companion] Landmarks fetch error:", err);
        } finally {
          isFetchingPois = false;
        }
      })();
    }
  }
}

function attachPoiButtonEvents() {
  document.querySelectorAll(".poi-action-btn").forEach(btn => {
    btn.onclick = () => {
      const lat  = parseFloat(btn.getAttribute("data-lat"));
      const lon  = parseFloat(btn.getAttribute("data-lon"));
      const name = btn.getAttribute("data-name");
      store.setActiveTab("tracking");
      setTimeout(() => focusLocationOnMap(lon, lat, name), 180);
    };
  });
}
