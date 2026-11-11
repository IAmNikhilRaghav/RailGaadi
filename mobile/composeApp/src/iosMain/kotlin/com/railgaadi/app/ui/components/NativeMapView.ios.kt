package com.railgaadi.app.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.railgaadi.app.model.Station
import com.railgaadi.app.util.PlatformSpeech
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
private class IosScriptBridge(
    private val onVoice: (String) -> Unit,
    private val onCompass: (Float) -> Unit,
    private val onNav: (Double, Double, String, String) -> Unit,
    private val onStation: (Station) -> Unit,
    private val onNavToStation: (Station) -> Unit
) : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage
    ) {
        val body = didReceiveScriptMessage.body
        if (body is Map<*, *>) {
            when (body["action"] as? String) {
                "speak" -> {
                    val text = (body["text"] as? String) ?: ""
                    if (text.isNotBlank()) {
                        onVoice(text)
                        PlatformSpeech.speak(text)
                    }
                }
                "onCompassUpdate" -> {
                    val b = (body["bearing"] as? Number)?.toFloat() ?: 0f
                    onCompass(b)
                }
                "onNavUpdate" -> {
                    val distM = (body["distM"] as? Number)?.toDouble() ?: 0.0
                    val durS = (body["durS"] as? Number)?.toDouble() ?: 0.0
                    val instr = (body["instruction"] as? String) ?: ""
                    val icon = (body["icon"] as? String) ?: "⬆"
                    onNav(distM, durS, instr, icon)
                }
                "onStationClick" -> {
                    val code = (body["code"] as? String) ?: ""
                    val name = (body["name"] as? String) ?: ""
                    val lat = (body["lat"] as? Number)?.toDouble() ?: 0.0
                    val lon = (body["lon"] as? Number)?.toDouble() ?: 0.0
                    val dep = (body["dep"] as? String) ?: "—"
                    val arr = (body["arr"] as? String) ?: "—"
                    val pf = (body["pf"] as? String) ?: "1"
                    val halt = (body["halt"] as? Number)?.toInt() ?: 2
                    onStation(
                        Station(
                            code = code,
                            name = name,
                            latitude = lat,
                            longitude = lon,
                            scheduledDep = dep,
                            scheduledArr = arr,
                            platform = pf,
                            haltMinutes = halt
                        )
                    )
                }
                "onNavigateToStation" -> {
                    val code = (body["code"] as? String) ?: ""
                    val name = (body["name"] as? String) ?: ""
                    val lat = (body["lat"] as? Number)?.toDouble() ?: 0.0
                    val lon = (body["lon"] as? Number)?.toDouble() ?: 0.0
                    onNavToStation(Station(code = code, name = name, latitude = lat, longitude = lon))
                }
            }
        } else if (body is String) {
            onVoice(body)
            PlatformSpeech.speak(body)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
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

    var lastHtmlContent by remember { mutableStateOf("") }
    var last3DMode by remember { mutableStateOf(is3DMode) }

    val bridge = remember(onVoiceInstruction, onCompassRotationChanged, onNavStatsUpdated, onStationClick, onNavigateToStation) {
        IosScriptBridge(
            onVoice = onVoiceInstruction,
            onCompass = onCompassRotationChanged,
            onNav = onNavStatsUpdated,
            onStation = onStationClick,
            onNavToStation = onNavigateToStation
        )
    }

    val webView = remember {
        val userContentController = WKUserContentController().apply {
            addScriptMessageHandler(bridge, name = "railBridge")
        }
        val config = WKWebViewConfiguration().apply {
            allowsInlineMediaPlayback = true
            this.userContentController = userContentController
        }
        WKWebView(frame = CGRectZero.readValue(), configuration = config)
    }

    UIKitView(
        modifier = modifier,
        factory = {
            webView.loadHTMLString(htmlContent, baseURL = NSURL(string = "https://railgaadi.app"))
            lastHtmlContent = htmlContent
            webView
        },
        update = { view ->
            if (lastHtmlContent != htmlContent) {
                lastHtmlContent = htmlContent
                view.loadHTMLString(htmlContent, baseURL = NSURL(string = "https://railgaadi.app"))
            } else if (last3DMode != is3DMode) {
                last3DMode = is3DMode
                view.evaluateJavaScript("if (window.set3DMode) window.set3DMode($is3DMode);", completionHandler = null)
            }
        }
    )
}

