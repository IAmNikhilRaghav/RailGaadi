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
        MapHtmlGenerator.generateMapHtml(
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
    var lastTrainNumber by remember { mutableStateOf(trainNumber) }
    var lastStationsJson by remember { mutableStateOf(stationsJson) }
    var lastMapStyle by remember { mutableStateOf(mapStyle) }

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
            val needsFullReload = lastNavMode != isNavigationMode ||
                lastDestStation.first != destLat ||
                lastDestStation.second != destLon ||
                lastTrainNumber != trainNumber ||
                lastStationsJson != stationsJson ||
                lastMapStyle != mapStyle

            if (needsFullReload) {
                lastNavMode = isNavigationMode
                lastDestStation = Pair(destLat, destLon)
                last3DMode = is3DMode
                lastTrainNumber = trainNumber
                lastStationsJson = stationsJson
                lastMapStyle = mapStyle
                webView.loadDataWithBaseURL("https://railgaadi.app", htmlContent, "text/html", "UTF-8", null)
            } else if (last3DMode != is3DMode) {
                last3DMode = is3DMode
                webView.evaluateJavascript("if (window.set3DMode) window.set3DMode($is3DMode);", null)
            }
        }
    )
}
