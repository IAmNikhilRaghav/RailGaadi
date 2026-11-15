package com.railgaadi.app.util

import android.content.Context
import android.content.SharedPreferences

actual object PlatformStorage {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("railgaadi_prefs", Context.MODE_PRIVATE)
        }
    }

    actual fun saveString(key: String, value: String) {
        try {
            prefs?.edit()?.putString(key, value)?.apply()
        } catch (e: Throwable) {
            println("PlatformStorage Android save error: ${e.message}")
        }
    }

    actual fun getString(key: String): String? {
        return try {
            prefs?.getString(key, null)
        } catch (_: Throwable) {
            null
        }
    }

    actual fun remove(key: String) {
        try {
            prefs?.edit()?.remove(key)?.apply()
        } catch (_: Throwable) {}
    }
}
