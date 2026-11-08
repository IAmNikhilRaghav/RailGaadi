package com.railgaadi.app.ui.components

import android.annotation.SuppressLint
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.railgaadi.app.config.AppConfig
import com.railgaadi.app.model.Station
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun NativeMapView(
    modifier: Modifier,
    isNavigationMode: Boolean,
    userLat: Double,
    userLon: Double,
    destLat: Double,
    destLon: Double,
    stationName: String,
    trainNumber: String,
    trainProgress: Float,
    stations: List<Station>,
    is3DMode: Boolean,
    followTrain: Boolean,
    mapStyle: String,
    onStationClick: (Station) -> Unit,
    onNavigateToStation: (Station) -> Unit,
    onVoiceInstruction: (String) -> Unit,
    onCompassRotationChanged: (Float) -> Unit,
    onNavStatsUpdated: (distM: Double, durS: Double, instruction: String, icon: String) -> Unit
) {
    val context = LocalContext.current
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    // Initialize Android Text-To-Speech for Turn-by-Turn Voice Directions
    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale("en", "IN")
            }
        }
        ttsInstance = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val stationsJson = remember(stations) {
        try {
            Json.encodeToString(stations)
        } catch (_: Exception) {
            "[]"
        }
    }

    val htmlContent = remember(
        isNavigationMode, userLat, userLon, destLat, destLon, stationName,
        trainNumber, trainProgress, stationsJson, is3DMode, followTrain, mapStyle
    ) {
        generateMapHtml(
            isNav = isNavigationMode,
            uLat = userLat,
            uLon = userLon,
            dLat = destLat,
            dLon = destLon,
            station = stationName,
            trainNo = trainNumber,
            progress = trainProgress,
            stationsJson = stationsJson,
            is3D = is3DMode,
            followTrain = followTrain,
            styleType = mapStyle
        )
    }

    var lastNavMode by remember { mutableStateOf(isNavigationMode) }
    var lastDestStation by remember { mutableStateOf(Pair(destLat, destLon)) }
    var last3DMode by remember { mutableStateOf(is3DMode) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    setGeolocationEnabled(true)
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        android.util.Log.d("RailMap", "${consoleMessage?.message()} [Line ${consoleMessage?.lineNumber()}]")
                        return super.onConsoleMessage(consoleMessage)
                    }

                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: GeolocationPermissions.Callback?
                    ) {
                        callback?.invoke(origin, true, false)
                    }
                }

                webViewClient = WebViewClient()

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onStationClick(code: String, name: String, lat: Double, lon: Double, dep: String, arr: String, pf: String, halt: Int) {
                        val st = Station(
                            code = code,
                            name = name,
                            latitude = lat,
                            longitude = lon,
                            scheduledDep = dep,
                            scheduledArr = arr,
                            platform = pf,
                            haltMinutes = halt
                        )
                        onStationClick(st)
                    }

                    @JavascriptInterface
                    fun onNavigateToStation(code: String, name: String, lat: Double, lon: Double) {
                        val st = Station(code = code, name = name, latitude = lat, longitude = lon)
                        onNavigateToStation(st)
                    }

                    @JavascriptInterface
                    fun onCompassUpdate(bearing: Float) {
                        onCompassRotationChanged(bearing)
                    }

                    @JavascriptInterface
                    fun onNavUpdate(distM: Double, durS: Double, instruction: String, icon: String) {
                        onNavStatsUpdated(distM, durS, instruction, icon)
                    }

                    @JavascriptInterface
                    fun speak(text: String) {
                        onVoiceInstruction(text)
                        ttsInstance?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NavGuidance")
                    }
                }, "RailBridge")

                loadDataWithBaseURL("https://railgaadi.app", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            if (lastNavMode != isNavigationMode || lastDestStation.first != destLat || lastDestStation.second != destLon) {
                lastNavMode = isNavigationMode
                lastDestStation = Pair(destLat, destLon)
                last3DMode = is3DMode
                webView.loadDataWithBaseURL("https://railgaadi.app", htmlContent, "text/html", "UTF-8", null)
            } else if (last3DMode != is3DMode) {
                last3DMode = is3DMode
                webView.evaluateJavascript("if (window.set3DMode) window.set3DMode($is3DMode);", null)
            }
        }
    )
}

private fun generateMapHtml(
    isNav: Boolean,
    uLat: Double,
    uLon: Double,
    dLat: Double,
    dLon: Double,
    station: String,
    trainNo: String,
    progress: Float,
    stationsJson: String,
    is3D: Boolean,
    followTrain: Boolean,
    styleType: String
): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/maplibre-gl@3.6.2/dist/maplibre-gl.css" />
    <script src="https://unpkg.com/maplibre-gl@3.6.2/dist/maplibre-gl.js"></script>
    <style>
        body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background: #e2e8f0; font-family: -apple-system, BlinkMacSystemFont, sans-serif; }
        #map { width: 100%; height: 100%; }
        
        .train-marker {
            width: 32px; height: 32px;
            background: #0f172a;
            color: #fff;
            border: 2px solid #ffffff;
            border-radius: 8px;
            display: flex; align-items: center; justify-content: center;
            box-shadow: 0 4px 12px rgba(15,23,42,0.35);
        }

        .user-marker {
            width: 28px; height: 28px;
            background: #2563eb;
            border: 2.5px solid #ffffff;
            border-radius: 50%;
            box-shadow: 0 0 0 5px rgba(37,99,235,0.22);
            transform-origin: center center;
        }

        .station-pin {
            width: 18px; height: 18px;
            background: #ffffff;
            border: 2.5px solid #0f172a;
            border-radius: 50%;
            cursor: pointer;
            box-shadow: 0 2px 6px rgba(0,0,0,0.25);
            transition: transform 0.15s ease;
        }
        .station-pin:hover { transform: scale(1.25); border-color: #2563eb; }

        .dest-marker {
            width: 32px; height: 32px;
            background: #0f172a;
            color: #fff;
            border: 2px solid #ffffff;
            border-radius: 50%;
            display: flex; align-items: center; justify-content: center;
            box-shadow: 0 4px 12px rgba(15,23,42,0.35);
        }

        /* Station Popup Styling */
        .maplibregl-popup-content {
            border-radius: 14px;
            padding: 12px 14px;
            box-shadow: 0 6px 20px rgba(0,0,0,0.18);
            font-family: inherit;
        }
        .popup-title { font-size: 14px; font-weight: 800; color: #0f172a; margin-bottom: 4px; }
        .popup-row { font-size: 11px; color: #475569; margin-bottom: 2px; }
        .popup-nav-btn {
            margin-top: 8px;
            width: 100%;
            background: #044e42;
            color: white;
            border: none;
            padding: 6px 10px;
            border-radius: 8px;
            font-weight: 700;
            font-size: 11px;
            cursor: pointer;
        }

        .maplibregl-ctrl-bottom-left, .maplibregl-ctrl-bottom-right { display: none !important; }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        const isNav = ${isNav};
        const uLat = ${uLat}, uLon = ${uLon};
        const dLat = ${dLat}, dLon = ${dLon};
        const is3D = ${is3D};
        const followTrain = ${followTrain};
        let inputStations = [];
        try {
            inputStations = ${stationsJson};
        } catch(e) {}

        const styleType = '${styleType}';
        const mapTilerKey = '${AppConfig.MAPTILER_API_KEY}';
        let styleUrl = 'https://api.maptiler.com/maps/streets-v2/style.json?key=' + mapTilerKey;
        if (styleType === 'dark') {
            styleUrl = 'https://api.maptiler.com/maps/dataviz-dark/style.json?key=' + mapTilerKey;
        } else if (styleType === 'satellite') {
            styleUrl = 'https://api.maptiler.com/maps/hybrid/style.json?key=' + mapTilerKey;
        } else if (styleType === 'terrain') {
            styleUrl = 'https://api.maptiler.com/maps/topo-v2/style.json?key=' + mapTilerKey;
        }

        const map = new maplibregl.Map({
            container: 'map',
            style: styleUrl,
            center: isNav ? [(uLon + dLon)/2, (uLat + dLat)/2] : [77.2090, 28.6139],
            zoom: isNav ? 12.8 : 6.5,
            pitch: is3D ? 58 : 0,
            bearing: 0,
            attributionControl: false,
            dragRotate: true,
            touchZoomRotate: true,
            scrollZoom: true,
            dragPan: true,
            doubleClickZoom: true,
            touchPitch: true
        });

        // Listen for compass rotation and send to Kotlin
        map.on('rotate', () => {
            const bearing = map.getBearing();
            if (window.RailBridge && window.RailBridge.onCompassUpdate) {
                window.RailBridge.onCompassUpdate(bearing);
            }
        });

        map.on('load', () => {
            if (is3D) {
                try {
                    const layers = map.getStyle().layers;
                    let labelLayerId;
                    for (let i = 0; i < layers.length; i++) {
                        if (layers[i].type === 'symbol' && layers[i].layout && layers[i].layout['text-field']) {
                            labelLayerId = layers[i].id;
                            break;
                        }
                    }
                    if (!map.getLayer('3d-buildings') && map.getSource('openmaptiles')) {
                        map.addLayer({
                            'id': '3d-buildings',
                            'source': 'openmaptiles',
                            'source-layer': 'building',
                            'type': 'fill-extrusion',
                            'minzoom': 14,
                            'paint': {
                                'fill-extrusion-color': '#cbd5e1',
                                'fill-extrusion-height': ['interpolate', ['linear'], ['zoom'], 14, 0, 16, ['get', 'render_height']],
                                'fill-extrusion-base': ['interpolate', ['linear'], ['zoom'], 14, 0, 16, ['get', 'render_min_height']],
                                'fill-extrusion-opacity': 0.7
                            }
                        }, labelLayerId);
                    }
                } catch(e) {}
            }

            if (isNav) {
                // NAVIGATION MODE
                const userEl = document.createElement('div');
                userEl.className = 'user-marker';
                new maplibregl.Marker({ element: userEl }).setLngLat([uLon, uLat]).addTo(map);

                const stationEl = document.createElement('div');
                stationEl.className = 'dest-marker';
                stationEl.innerHTML = '<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="3" width="16" height="15" rx="2"/><line x1="4" y1="11" x2="20" y2="11"/><circle cx="8" cy="15" r="1.5" fill="#ffffff"/><circle cx="16" cy="15" r="1.5" fill="#ffffff"/><line x1="8" y1="19" x2="6" y2="22"/><line x1="16" y1="19" x2="18" y2="22"/></svg>';
                new maplibregl.Marker({ element: stationEl }).setLngLat([dLon, dLat]).addTo(map);

                // Fetch OSRM Road Route with Step-by-Step Directions
                const osrmUrl = 'https://router.project-osrm.org/route/v1/driving/' + uLon + ',' + uLat + ';' + dLon + ',' + dLat + '?steps=true&overview=full&geometries=geojson';
                fetch(osrmUrl)
                    .then(res => res.json())
                    .then(data => {
                        if (data.routes && data.routes[0]) {
                            const route = data.routes[0];
                            const coordinates = route.geometry.coordinates;

                            map.addSource('nav-route', {
                                type: 'geojson',
                                data: {
                                    type: 'Feature',
                                    geometry: { type: 'LineString', coordinates: coordinates }
                                }
                            });

                            // Road Casing (Dark Blue/Indigo Border)
                            map.addLayer({
                                id: 'nav-casing',
                                type: 'line',
                                source: 'nav-route',
                                layout: { 'line-join': 'round', 'line-cap': 'round' },
                                paint: { 'line-color': '#1e3a8a', 'line-width': 10 }
                            });

                            // Main Navigation Purple Line
                            map.addLayer({
                                id: 'nav-line',
                                type: 'line',
                                source: 'nav-route',
                                layout: { 'line-join': 'round', 'line-cap': 'round' },
                                paint: { 'line-color': '#3b82f6', 'line-width': 6.5 }
                            });

                            // Real GPS User Orientation Marker (moves only when user moves)
                            const gpsMarkerEl = document.createElement('div');
                            gpsMarkerEl.className = 'user-marker';
                            const gpsMarker = new maplibregl.Marker({
                                element: gpsMarkerEl,
                                rotationAlignment: 'map',
                                rotation: 0
                            }).setLngLat([uLon, uLat]).addTo(map);

                            if (navigator.geolocation) {
                                navigator.geolocation.watchPosition(
                                    function(pos) {
                                        const lat = pos.coords.latitude;
                                        const lon = pos.coords.longitude;
                                        const heading = pos.coords.heading != null ? pos.coords.heading : 0;
                                        gpsMarker.setLngLat([lon, lat]);
                                        gpsMarker.setRotation(heading);
                                        gpsMarkerEl.style.transform = 'rotate(' + heading + 'deg)';
                                    },
                                    null,
                                    { enableHighAccuracy: true, maximumAge: 2000, timeout: 10000 }
                                );
                            }

                            // Fit bounds to route
                            const bounds = new maplibregl.LngLatBounds();
                            coordinates.forEach(coord => bounds.extend(coord));
                            map.fitBounds(bounds, { padding: 90, duration: 1200 });

                            // Extract Turn-by-Turn Instruction
                            const legs = route.legs || [];
                            const steps = legs[0]?.steps || [];
                            let firstTurnInstruction = "Head northwest";
                            let maneuverIcon = "⬆";

                            if (steps.length > 1) {
                                const nextStep = steps[1];
                                const m = nextStep.maneuver || {};
                                const modifier = m.modifier || "";
                                const name = nextStep.name || "Station Road";

                                if (modifier.includes("right")) {
                                    maneuverIcon = "↱";
                                    firstTurnInstruction = "Turn right onto " + name;
                                } else if (modifier.includes("left")) {
                                    maneuverIcon = "↰";
                                    firstTurnInstruction = "Turn left onto " + name;
                                } else if (modifier.includes("straight")) {
                                    maneuverIcon = "⬆";
                                    firstTurnInstruction = "Continue straight on " + name;
                                }
                            }

                            // Pass stats & voice guidance to Android
                            if (window.RailBridge && window.RailBridge.onNavUpdate) {
                                window.RailBridge.onNavUpdate(route.distance, route.duration, firstTurnInstruction, maneuverIcon);
                            }
                            if (window.RailBridge && window.RailBridge.speak) {
                                window.RailBridge.speak(firstTurnInstruction + " toward " + "${station}");
                            }
                        }
                    })
                    .catch(() => {
                        // Fallback line
                        map.addSource('nav-route', {
                            type: 'geojson',
                            data: {
                                type: 'Feature',
                                geometry: { type: 'LineString', coordinates: [[uLon, uLat], [dLon, dLat]] }
                            }
                        });
                        map.addLayer({
                            id: 'nav-line',
                            type: 'line',
                            source: 'nav-route',
                            paint: { 'line-color': '#4f46e5', 'line-width': 6 }
                        });
                    });
            } else {
                // TRAIN TRACK MODE
                const defaultTrack = [
                    { code: "LGNH", name: "Lalgarh Jn", lat: 28.0559, lon: 73.2819, dep: "10:30", arr: "—", pf: "1", halt: 0 },
                    { code: "BKN",  name: "Bikaner Jn", lat: 28.0180, lon: 73.3168, dep: "10:55", arr: "10:45", pf: "2", halt: 10 },
                    { code: "JP",   name: "Jaipur Jn", lat: 26.9196, lon: 75.7878, dep: "15:20", arr: "15:10", pf: "4", halt: 10 },
                    { code: "BTE",  name: "Bharatpur Jn", lat: 27.2152, lon: 77.4930, dep: "18:15", arr: "18:10", pf: "3", halt: 5 },
                    { code: "AF",   name: "Agra Fort", lat: 27.1767, lon: 78.0081, dep: "19:25", arr: "19:15", pf: "1", halt: 10 },
                    { code: "TDL",  name: "Tundla Jn", lat: 27.2064, lon: 78.2415, dep: "20:40", arr: "20:35", pf: "5", halt: 5 },
                    { code: "CNB",  name: "Kanpur Central", lat: 26.4547, lon: 80.3507, dep: "00:45", arr: "00:35", pf: "7", halt: 10 },
                    { code: "PRYJ", name: "Prayagraj Jn", lat: 25.4358, lon: 81.8463, dep: "—", arr: "04:45", pf: "2", halt: 0 }
                ];

                const stationsToRender = (inputStations && inputStations.length > 0) ? inputStations : defaultTrack;
                const trackCoords = stationsToRender.map(s => [s.longitude || s.lon, s.latitude || s.lat]);

                map.addSource('rail-track', {
                    type: 'geojson',
                    data: {
                        type: 'Feature',
                        geometry: { type: 'LineString', coordinates: trackCoords }
                    }
                });

                // Track Base line
                map.addLayer({
                    id: 'rail-casing',
                    type: 'line',
                    source: 'rail-track',
                    paint: { 'line-color': '#0f172a', 'line-width': 7 }
                });

                // Dashed Rail Pattern
                map.addLayer({
                    id: 'rail-dashed',
                    type: 'line',
                    source: 'rail-track',
                    paint: {
                        'line-color': '#38bdf8',
                        'line-width': 3.5,
                        'line-dasharray': [2, 2]
                    }
                });

                // Interactive Station Pins with Popups
                stationsToRender.forEach(st => {
                    const el = document.createElement('div');
                    el.className = 'station-pin';

                    const popupHtml = `
                        <div class="popup-title">🚉 ` + st.name + ` (` + st.code + `)</div>
                        <div class="popup-row">⏱️ Arr: ` + (st.scheduledArr || st.arr || "—") + ` | Dep: ` + (st.scheduledDep || st.dep || "—") + `</div>
                        <div class="popup-row">🚉 Platform: PF ` + (st.platform || "1") + ` | Halt: ` + (st.haltMinutes || st.halt || 2) + ` min</div>
                        <button class="popup-nav-btn" onclick="triggerNav('` + st.code + `','` + st.name + `',` + (st.latitude || st.lat) + `,` + (st.longitude || st.lon) + `)">🧭 Directions to this Station</button>
                    `;

                    const popup = new maplibregl.Popup({ offset: 12 }).setHTML(popupHtml);

                    new maplibregl.Marker({ element: el })
                        .setLngLat([st.longitude || st.lon, st.latitude || st.lat])
                        .setPopup(popup)
                        .addTo(map);

                    el.addEventListener('click', () => {
                        if (window.RailBridge && window.RailBridge.onStationClick) {
                            window.RailBridge.onStationClick(
                                st.code, st.name,
                                st.latitude || st.lat, st.longitude || st.lon,
                                st.scheduledDep || st.dep || "—",
                                st.scheduledArr || st.arr || "—",
                                st.platform || "1",
                                st.haltMinutes || st.halt || 2
                            );
                        }
                    });
                });

                // Train Marker (Clean Minimalist Vector)
                const trainEl = document.createElement('div');
                trainEl.className = 'train-marker';
                trainEl.innerHTML = '<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="3" width="16" height="15" rx="2"/><line x1="4" y1="11" x2="20" y2="11"/><circle cx="8" cy="15" r="1.5" fill="#ffffff"/><circle cx="16" cy="15" r="1.5" fill="#ffffff"/><line x1="8" y1="19" x2="6" y2="22"/><line x1="16" y1="19" x2="18" y2="22"/></svg>';

                const progressClamped = Math.max(0, Math.min(1, ${progress}));
                const totalSegments = trackCoords.length - 1;
                const exactIdx = progressClamped * totalSegments;
                const baseIdx = Math.min(Math.floor(exactIdx), totalSegments - 1);
                const frac = exactIdx - baseIdx;

                const p1 = trackCoords[baseIdx];
                const p2 = trackCoords[baseIdx + 1];
                const trainLon = p1[0] + (p2[0] - p1[0]) * frac;
                const trainLat = p1[1] + (p2[1] - p1[1]) * frac;

                new maplibregl.Marker({ element: trainEl }).setLngLat([trainLon, trainLat]).addTo(map);

                // Auto-center or Follow Mode
                if (followTrain) {
                    map.easeTo({ center: [trainLon, trainLat], zoom: 8.5, duration: 1500 });
                } else {
                    map.flyTo({ center: [trainLon, trainLat], zoom: 7, duration: 1200 });
                }
            }
        });

        window.triggerNav = function(code, name, lat, lon) {
            if (window.RailBridge && window.RailBridge.onNavigateToStation) {
                window.RailBridge.onNavigateToStation(code, name, lat, lon);
            }
        };

        window.resetNorth = function() {
            map.easeTo({ bearing: 0, duration: 500 });
        };

        window.rotateMap = function(deltaDeg) {
            map.easeTo({ bearing: map.getBearing() + deltaDeg, duration: 300 });
        };

        window.zoomInMap = function() {
            map.zoomIn({ duration: 300 });
        };

        window.zoomOutMap = function() {
            map.zoomOut({ duration: 300 });
        };

        window.set3DMode = function(enable3D) {
            map.easeTo({ pitch: enable3D ? 58 : 0, duration: 600 });
        };
    </script>
</body>
</html>
    """.trimIndent()
}
