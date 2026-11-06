package com.railgaadi.app.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

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
    trainProgress: Float
) {
    val htmlContent = remember(isNavigationMode, userLat, userLon, destLat, destLon, trainProgress) {
        generateMapHtml(isNavigationMode, userLat, userLon, destLat, destLon, stationName, trainNumber, trainProgress)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://railgaadi.app", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://railgaadi.app", htmlContent, "text/html", "UTF-8", null)
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
    progress: Float
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
            width: 38px; height: 38px;
            background: #2563eb;
            color: #fff;
            border: 2.5px solid #fff;
            border-radius: 50%;
            display: flex; align-items: center; justify-content: center;
            font-size: 19px;
            box-shadow: 0 4px 14px rgba(37,99,235,0.45);
        }
        .user-marker {
            width: 22px; height: 22px;
            background: #0284c7;
            border: 3.5px solid #ffffff;
            border-radius: 50%;
            box-shadow: 0 0 0 6px rgba(2,132,199,0.3);
        }
        .station-marker {
            width: 32px; height: 32px;
            background: #dc2626;
            color: #fff;
            border: 2px solid #fff;
            border-radius: 50%;
            display: flex; align-items: center; justify-content: center;
            font-size: 15px;
            box-shadow: 0 3px 10px rgba(220,38,38,0.4);
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
        
        const map = new maplibregl.Map({
            container: 'map',
            style: {
                version: 8,
                sources: {
                    'osm': {
                        type: 'raster',
                        tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
                        tileSize: 256,
                        attribution: '&copy; OpenStreetMap'
                    }
                },
                layers: [{
                    id: 'osm-layer',
                    type: 'raster',
                    source: 'osm',
                    minzoom: 0,
                    maxzoom: 19
                }]
            },
            center: isNav ? [(uLon + dLon)/2, (uLat + dLat)/2] : [77.2090, 28.6139],
            zoom: isNav ? 12.5 : 6,
            attributionControl: false
        });

        map.on('load', () => {
            if (isNav) {
                // NAVIGATION MODE: User to Station Route
                const userEl = document.createElement('div');
                userEl.className = 'user-marker';
                new maplibregl.Marker({ element: userEl }).setLngLat([uLon, uLat]).addTo(map);

                const stationEl = document.createElement('div');
                stationEl.className = 'station-marker';
                stationEl.innerHTML = '🚉';
                new maplibregl.Marker({ element: stationEl }).setLngLat([dLon, dLat]).addTo(map);

                // Fetch OSRM Road Route
                const osrmUrl = 'https://router.project-osrm.org/route/v1/driving/' + uLon + ',' + uLat + ';' + dLon + ',' + dLat + '?overview=full&geometries=geojson';
                fetch(osrmUrl)
                    .then(res => res.json())
                    .then(data => {
                        const coordinates = (data.routes && data.routes[0]) 
                            ? data.routes[0].geometry.coordinates 
                            : [[uLon, uLat], [dLon, dLat]];

                        map.addSource('route', {
                            type: 'geojson',
                            data: {
                                type: 'Feature',
                                geometry: { type: 'LineString', coordinates: coordinates }
                            }
                        });

                        // Road Casing (Dark Blue/Indigo Border)
                        map.addLayer({
                            id: 'route-casing',
                            type: 'line',
                            source: 'route',
                            layout: { 'line-join': 'round', 'line-cap': 'round' },
                            paint: { 'line-color': '#1e3a8a', 'line-width': 9 }
                        });

                        // Main Navigation Purple Line (Google Maps style)
                        map.addLayer({
                            id: 'route-line',
                            type: 'line',
                            source: 'route',
                            layout: { 'line-join': 'round', 'line-cap': 'round' },
                            paint: { 'line-color': '#3b82f6', 'line-width': 6 }
                        });

                        const bounds = new maplibregl.LngLatBounds();
                        coordinates.forEach(coord => bounds.extend(coord));
                        map.fitBounds(bounds, { padding: 80, duration: 1000 });
                    })
                    .catch(() => {
                        // Fallback straight purple line
                        map.addSource('route', {
                            type: 'geojson',
                            data: {
                                type: 'Feature',
                                geometry: { type: 'LineString', coordinates: [[uLon, uLat], [dLon, dLat]] }
                            }
                        });
                        map.addLayer({
                            id: 'route-line',
                            type: 'line',
                            source: 'route',
                            paint: { 'line-color': '#4f46e5', 'line-width': 6 }
                        });
                    });
            } else {
                // TRAIN TRACK MODE: Railway Geometry & Live Marker
                const trackCoords = [
                    [73.3119, 28.0159], // Lalgarh
                    [73.3168, 28.0180], // Bikaner
                    [74.6399, 26.4499], // Ajmer
                    [75.8189, 26.9196], // Jaipur
                    [77.6737, 27.4924], // Mathura
                    [78.0081, 27.1767], // Agra Fort
                    [80.3319, 26.4499], // Kanpur Central
                    [81.8463, 25.4358]  // Prayagraj
                ];

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
                    paint: { 'line-color': '#0f172a', 'line-width': 6 }
                });

                // Dashed Rail Pattern
                map.addLayer({
                    id: 'rail-dashed',
                    type: 'line',
                    source: 'rail-track',
                    paint: {
                        'line-color': '#38bdf8',
                        'line-width': 3,
                        'line-dasharray': [2, 2]
                    }
                });

                // Train Marker
                const trainEl = document.createElement('div');
                trainEl.className = 'train-marker';
                trainEl.innerHTML = '🚆';
                
                // Interpolate along route based on progress
                const idx = Math.min(Math.floor(${progress} * (trackCoords.length - 1)), trackCoords.length - 2);
                const trainPos = trackCoords[idx];
                new maplibregl.Marker({ element: trainEl }).setLngLat(trainPos).addTo(map);

                // Add Station markers
                trackCoords.forEach((coord, i) => {
                    const stEl = document.createElement('div');
                    stEl.style.width = '10px';
                    stEl.style.height = '10px';
                    stEl.style.backgroundColor = '#ffffff';
                    stEl.style.border = '2.5px solid #0f172a';
                    stEl.style.borderRadius = '50%';
                    new maplibregl.Marker({ element: stEl }).setLngLat(coord).addTo(map);
                });

                map.flyTo({ center: trainPos, zoom: 7, duration: 1500 });
            }
        });
    </script>
</body>
</html>
    """.trimIndent()
}
