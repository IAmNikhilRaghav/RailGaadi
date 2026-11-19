package com.railgaadi.app.util

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

actual object PlatformShare {
    actual fun shareText(text: String) {
        val activityItems = listOf(text)
        val controller = UIActivityViewController(activityItems, null)
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(controller, animated = true, completion = null)
    }

    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }
}
