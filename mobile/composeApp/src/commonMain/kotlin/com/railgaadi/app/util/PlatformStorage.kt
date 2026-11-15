package com.railgaadi.app.util

expect object PlatformStorage {
    fun saveString(key: String, value: String)
    fun getString(key: String): String?
    fun remove(key: String)
}
