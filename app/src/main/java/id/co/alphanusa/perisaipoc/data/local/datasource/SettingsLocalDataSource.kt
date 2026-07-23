package id.co.alphanusa.perisaipoc.data.local.datasource

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import id.co.alphanusa.perisaipoc.BuildConfig
import id.co.alphanusa.perisaipoc.core.util.Constants
import id.co.alphanusa.perisaipoc.domain.model.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Sumber konfigurasi endpoint aplikasi (base URL, Centrifugo, RTMP, LiveKit). */
interface SettingsLocalDataSource {
    val config: StateFlow<AppConfig>
    fun getConfig(): AppConfig
    fun updateConfig(config: AppConfig)
    fun resetToDefaults()
}

/**
 * Menyimpan konfigurasi di SharedPreferences dan memaparkannya sebagai
 * [StateFlow] agar perubahan (mis. setelah scan QR) langsung terlihat oleh
 * komponen lain tanpa perlu membangun ulang Retrofit.
 */
@Singleton
class PrefsSettingsLocalDataSource @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsLocalDataSource {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.Prefs.SETTINGS_NAME, Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(readConfig())
    override val config: StateFlow<AppConfig> = _config.asStateFlow()

    override fun getConfig(): AppConfig = _config.value

    override fun updateConfig(config: AppConfig) {
        prefs.edit()
            .putString(Constants.Prefs.KEY_BASE_URL, config.baseUrl)
            .putString(Constants.Prefs.KEY_CENTRIFUGO_WS_URL, config.centrifugoWebSocketUrl)
            .putString(Constants.Prefs.KEY_RTMP_URL, config.rtmpUrl)
            .putString(Constants.Prefs.KEY_LIVEKIT_URL, config.livekitUrl)
            .apply()
        _config.value = config
    }

    override fun resetToDefaults() {
        prefs.edit().clear().apply()
        _config.value = readConfig()
    }

    private fun readConfig() = AppConfig(
        baseUrl = prefs.getString(Constants.Prefs.KEY_BASE_URL, BuildConfig.BASE_URL)
            ?: BuildConfig.BASE_URL,
        centrifugoWebSocketUrl = prefs.getString(
            Constants.Prefs.KEY_CENTRIFUGO_WS_URL,
            BuildConfig.CENTRIFUGO_WEBSOCKET_URL,
        ) ?: BuildConfig.CENTRIFUGO_WEBSOCKET_URL,
        rtmpUrl = prefs.getString(Constants.Prefs.KEY_RTMP_URL, BuildConfig.RTMP_URL)
            ?: BuildConfig.RTMP_URL,
        livekitUrl = prefs.getString(Constants.Prefs.KEY_LIVEKIT_URL, BuildConfig.LIVEKIT_URL)
            ?: BuildConfig.LIVEKIT_URL,
    )
}
