package com.railgaadi.app.util

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

/**
 * Modifier extension that automatically clears focus and hides the software keyboard
 * whenever the user taps anywhere on the component (or unconsumed taps from children bubble up).
 *
 * This replicates standard enterprise mobile UX (iOS / Android), ensuring the keyboard
 * smoothly dismisses when tapping on background areas, cards, text labels, or display space.
 */
fun Modifier.clearFocusOnTap(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    pointerInput(focusManager, keyboardController) {
        detectTapGestures(
            onTap = {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            }
        )
    }
}

/**
 * Explicit variant of clearFocusOnTap when FocusManager and/or SoftwareKeyboardController
 * are already available in scope.
 */
fun Modifier.clearFocusOnTap(
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController? = null
): Modifier = this.pointerInput(focusManager, keyboardController) {
    detectTapGestures(
        onTap = {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    )
}
