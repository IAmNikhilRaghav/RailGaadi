package com.railgaadi.app.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.railgaadi.app.model.Station
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSURL
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

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

    val webView = remember {
        val config = WKWebViewConfiguration().apply {
            allowsInlineMediaPlayback = true
        }
        WKWebView()
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
