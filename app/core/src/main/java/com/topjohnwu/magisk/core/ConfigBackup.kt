package com.topjohnwu.magisk.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Exports only fork-owned preferences that are safe and useful to move
 * between installations. Root credentials and Magisk database values are
 * deliberately excluded.
 */
object ConfigBackup {
    private const val SCHEMA_VERSION = 1
    private const val MAX_BACKUP_SIZE = 1024 * 1024

    private const val SCHEMA = "schema"
    private const val SETTINGS = "settings"
    private const val ACCENT_COLOR = "accent_color"
    private const val COLOR_MODE = "color_mode"
    private const val DARK_THEME = "dark_theme"
    private const val LOCALE = "locale"
    private const val DOH = "doh"
    private const val DOWNLOAD_DIR = "download_dir"
    private const val RAND_NAME = "random_package_name"
    private const val CHECK_UPDATES = "check_updates"
    private const val MODULE_FAVORITES = "module_favorites"

    fun export(): String {
        val favorites = JSONArray()
        Config.moduleFavorites.sorted().forEach(favorites::put)

        return JSONObject().apply {
            put(SCHEMA, SCHEMA_VERSION)
            put(SETTINGS, JSONObject().apply {
                put(ACCENT_COLOR, Config.accentColor)
                put(COLOR_MODE, Config.colorMode)
                put(DARK_THEME, Config.darkTheme)
                put(LOCALE, Config.locale)
                put(DOH, Config.doh)
                put(DOWNLOAD_DIR, Config.downloadDir)
                put(RAND_NAME, Config.randName)
                put(CHECK_UPDATES, Config.checkUpdate)
                put(MODULE_FAVORITES, favorites)
            })
        }.toString(2)
    }

    /** Returns true only after the complete backup has been validated. */
    fun restore(serialized: String): Boolean {
        if (serialized.length > MAX_BACKUP_SIZE) return false

        return runCatching {
            val root = JSONObject(serialized)
            if (root.optInt(SCHEMA, -1) != SCHEMA_VERSION) return false
            val settings = root.optJSONObject(SETTINGS) ?: return false

            val favorites = settings.optJSONArray(MODULE_FAVORITES)?.let { array ->
                buildSet {
                    for (index in 0 until array.length()) {
                        array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
            }

            val accentColor = settings.optIntOrNull(ACCENT_COLOR)
            val colorMode = settings.optIntOrNull(COLOR_MODE)
            val darkTheme = settings.optIntOrNull(DARK_THEME)
            val locale = settings.optStringOrNull(LOCALE)
            val doh = settings.optBooleanOrNull(DOH)
            val downloadDir = settings.optStringOrNull(DOWNLOAD_DIR)
            val randName = settings.optBooleanOrNull(RAND_NAME)
            val checkUpdates = settings.optBooleanOrNull(CHECK_UPDATES)

            accentColor?.let { Config.accentColor = it }
            colorMode?.let { Config.colorMode = it }
            darkTheme?.let { Config.darkTheme = it }
            locale?.let { Config.locale = it }
            doh?.let { Config.doh = it }
            downloadDir?.let { Config.downloadDir = it }
            randName?.let { Config.randName = it }
            checkUpdates?.let { Config.checkUpdate = it }
            favorites?.let { Config.moduleFavorites = it }
            true
        }.getOrDefault(false)
    }

    private fun JSONObject.optIntOrNull(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null

    private fun JSONObject.optBooleanOrNull(name: String): Boolean? =
        if (has(name) && !isNull(name)) optBoolean(name) else null

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (has(name) && !isNull(name)) optString(name) else null
}
