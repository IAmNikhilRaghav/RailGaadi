package com.railgaadi.app.util

import platform.Foundation.NSUserDefaults

actual object PlatformStorage {
    actual fun saveString(key: String, value: String) {
        try {
            val defaults = NSUserDefaults.standardUserDefaults
            defaults.setObject(value, forKey = key)
            defaults.synchronize()
        } catch (e: Throwable) {
            println("PlatformStorage iOS save error: ${e.message}")
        }
    }

    actual fun getString(key: String): String? {
        return try {
            NSUserDefaults.standardUserDefaults.stringForKey(key)
        } catch (_: Throwable) {
            null
        }
    }

    actual fun remove(key: String) {
        try {
            val defaults = NSUserDefaults.standardUserDefaults
            defaults.removeObjectForKey(key)
            defaults.synchronize()
        } catch (_: Throwable) {}
    }
}
