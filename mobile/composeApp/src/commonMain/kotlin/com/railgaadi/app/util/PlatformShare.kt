package com.railgaadi.app.util

/** Cross-platform Share & Clipboard utility */
expect object PlatformShare {
    /** Share text via the native OS share sheet */
    fun shareText(text: String)
    /** Copy text to the system clipboard */
    fun copyToClipboard(text: String)
}
