package id.co.alphanusa.perisaipoc.domain.repository

import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.domain.model.AppConfig
import id.co.alphanusa.perisaipoc.domain.model.CallParticipant
import id.co.alphanusa.perisaipoc.domain.model.CallRoom
import id.co.alphanusa.perisaipoc.domain.model.MapBounds
import id.co.alphanusa.perisaipoc.domain.model.MapOverlayItem
import id.co.alphanusa.perisaipoc.domain.model.PocDevice
import id.co.alphanusa.perisaipoc.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

/** Profil pengguna yang sedang login. */
interface UserRepository {
    suspend fun getProfile(): AppResult<UserProfile>
}

/** Identitas perangkat POC + URL tujuan streaming. */
interface PocRepository {
    suspend fun getDevice(): AppResult<PocDevice>

    /** Menyusun URL RTMP lengkap beserta kredensial. */
    suspend fun buildStreamUrl(): AppResult<String>
}

/** Overlay gambar pada peta. */
interface MapRepository {
    suspend fun getOverlay(bounds: MapBounds): AppResult<List<MapOverlayItem>>

    /** Mengunduh berkas ikon pin (butuh header auth). */
    suspend fun loadStickerImage(iconId: String): AppResult<ByteArray>
}

/** Komunikasi suara (LiveKit). */
interface CallRepository {
    suspend fun joinRoom(): AppResult<CallRoom>
    suspend fun getParticipants(): AppResult<List<CallParticipant>>
}

/** Token koneksi realtime (Centrifugo). */
interface RealtimeRepository {
    suspend fun generateConnectionToken(): AppResult<String>
}

/** Konfigurasi endpoint aplikasi. */
interface SettingsRepository {
    val config: StateFlow<AppConfig>
    fun getConfig(): AppConfig
    fun updateConfig(config: AppConfig)
    fun resetToDefaults()
}
