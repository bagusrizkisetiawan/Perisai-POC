package id.co.alphanusa.perisaipoc.domain.usecase

import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.domain.model.AppConfig
import id.co.alphanusa.perisaipoc.domain.model.CallParticipant
import id.co.alphanusa.perisaipoc.domain.model.CallRoom
import id.co.alphanusa.perisaipoc.domain.model.MapBounds
import id.co.alphanusa.perisaipoc.domain.model.MapOverlayItem
import id.co.alphanusa.perisaipoc.domain.model.UserProfile
import id.co.alphanusa.perisaipoc.domain.repository.CallRepository
import id.co.alphanusa.perisaipoc.domain.repository.MapRepository
import id.co.alphanusa.perisaipoc.domain.repository.PocRepository
import id.co.alphanusa.perisaipoc.domain.repository.RealtimeRepository
import id.co.alphanusa.perisaipoc.domain.repository.SettingsRepository
import id.co.alphanusa.perisaipoc.domain.repository.UserRepository
import javax.inject.Inject

/** Mengambil profil pengguna untuk ditampilkan di status bar. */
class GetUserProfileUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(): AppResult<UserProfile> = repository.getProfile()
}

/** Menyusun URL RTMP tujuan streaming beserta kredensialnya. */
class BuildStreamUrlUseCase @Inject constructor(
    private val repository: PocRepository,
) {
    suspend operator fun invoke(): AppResult<String> = repository.buildStreamUrl()
}

/** Mengambil token koneksi Centrifugo. */
class GenerateRealtimeTokenUseCase @Inject constructor(
    private val repository: RealtimeRepository,
) {
    suspend operator fun invoke(): AppResult<String> = repository.generateConnectionToken()
}

/** Mengambil overlay peta pada area yang sedang dilihat. */
class GetMapOverlayUseCase @Inject constructor(
    private val repository: MapRepository,
) {
    suspend operator fun invoke(bounds: MapBounds): AppResult<List<MapOverlayItem>> =
        repository.getOverlay(bounds)
}

/** Mengunduh berkas ikon pin peta. */
class LoadStickerImageUseCase @Inject constructor(
    private val repository: MapRepository,
) {
    suspend operator fun invoke(iconId: String): AppResult<ByteArray> =
        repository.loadStickerImage(iconId)
}

/** Bergabung ke room LiveKit dan mendapatkan tokennya. */
class JoinCallUseCase @Inject constructor(
    private val repository: CallRepository,
) {
    suspend operator fun invoke(): AppResult<CallRoom> = repository.joinRoom()
}

/** Mengambil daftar peserta call. */
class GetCallParticipantsUseCase @Inject constructor(
    private val repository: CallRepository,
) {
    suspend operator fun invoke(): AppResult<List<CallParticipant>> = repository.getParticipants()
}

/** Membaca konfigurasi endpoint aplikasi. */
class GetAppConfigUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): AppConfig = repository.getConfig()
}

/** Menyimpan perubahan konfigurasi endpoint. */
class UpdateAppConfigUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(config: AppConfig) = repository.updateConfig(config)
}
