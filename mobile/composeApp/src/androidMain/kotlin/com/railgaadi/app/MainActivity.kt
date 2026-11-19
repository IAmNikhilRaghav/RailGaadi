package com.railgaadi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import com.railgaadi.app.util.PlatformAlarm
import com.railgaadi.app.util.PlatformCellScanner
import com.railgaadi.app.util.PlatformLocation
import com.railgaadi.app.util.PlatformShare
import com.railgaadi.app.util.PlatformSpeech
import com.railgaadi.app.util.PlatformStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformStorage.init(this)
        PlatformSpeech.init(this)
        PlatformAlarm.init(this)
        PlatformLocation.init(this)
        PlatformCellScanner.init(this)
        PlatformShare.init(this)
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

