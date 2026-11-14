package club.cloudless.helios.config

import android.content.Context
import android.content.SharedPreferences
import club.cloudless.helios.data.WidgetSnapshot
import kotlinx.serialization.json.Json

/**
 * Server URL, API token and the last snapshot the widget has to render.
 *
 * Both the TWA and the widget read the same values: the token comes from Helios
 * Settings (API tokens) and is sent as `Authorization: Bearer`.
 */
class ConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("helios", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "")?.trim().orEmpty()
        set(value) = prefs.edit().putString(KEY_BASE_URL, value.trim().trimEnd('/')).apply()

    var token: String
        get() = prefs.getString(KEY_TOKEN, "")?.trim().orEmpty()
        set(value) = prefs.edit().putString(KEY_TOKEN, value.trim()).apply()

    fun isConfigured(): Boolean = baseUrl.startsWith("http") && token.isNotEmpty()

    fun saveSnapshot(snapshot: WidgetSnapshot) {
        prefs.edit()
            .putString(KEY_SNAPSHOT, json.encodeToString(WidgetSnapshot.serializer(), snapshot))
            .putString(KEY_ERROR, "")
            .apply()
    }

    fun snapshot(): WidgetSnapshot? {
        val raw = prefs.getString(KEY_SNAPSHOT, null) ?: return null
        return runCatching { json.decodeFromString(WidgetSnapshot.serializer(), raw) }.getOrNull()
    }

    fun saveError(message: String) {
        prefs.edit().putString(KEY_ERROR, message).apply()
    }

    fun clearError() {
        prefs.edit().putString(KEY_ERROR, "").apply()
    }

    fun error(): String = prefs.getString(KEY_ERROR, "").orEmpty()

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_SNAPSHOT = "snapshot"
        private const val KEY_ERROR = "error"

        fun create(context: Context): ConfigStore = ConfigStore(context)
    }
}
