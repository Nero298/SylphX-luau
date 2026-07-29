package com.sylphx.luau

import android.content.Context

/**
 * Persists the user's chosen animated theme and optional custom background
 * image path (copied into internal storage so it survives across app
 * restarts and content:// URI permission revocation).
 */
object ThemeManager {

    private const val PREFS = "sylphx_theme_prefs"
    private const val KEY_THEME_ID = "theme_id"
    private const val KEY_BG_PATH = "custom_bg_path"

    fun getSelectedTheme(context: Context): AppTheme {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AppTheme.byId(prefs.getString(KEY_THEME_ID, null))
    }

    fun setSelectedTheme(context: Context, theme: AppTheme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_ID, theme.id)
            .apply()
    }

    fun getCustomBackgroundPath(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_BG_PATH, null)
        return if (path != null && java.io.File(path).exists()) path else null
    }

    fun setCustomBackgroundPath(context: Context, path: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BG_PATH, path)
            .apply()
    }

    fun clearCustomBackground(context: Context) {
        getCustomBackgroundPath(context)?.let { java.io.File(it).delete() }
        setCustomBackgroundPath(context, null)
    }
}
