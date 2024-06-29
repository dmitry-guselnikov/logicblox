package net.guselnikov.logicblox.datasource

import android.content.Context
import android.content.SharedPreferences

interface SnippetsDataSource {
    fun saveCodeSnippet(id: String, snippet: String)
    fun getCodeSnippet(id: String): String
}

class PreferencesSnippetsDataSource(context: Context): SnippetsDataSource {
    companion object {
        const val PREFS_NAME = "SNIPPETS_PREFS"
        const val SNIPPET_PREFIX = "SNIPPET_"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, 0)

    override fun saveCodeSnippet(id: String, snippet: String) {
        val key = "$SNIPPET_PREFIX$id"
        preferences.edit().putString(key, snippet).apply()
    }

    override fun getCodeSnippet(id: String): String = preferences.getString("$SNIPPET_PREFIX$id", null) ?: ""
}