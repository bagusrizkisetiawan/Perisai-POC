package id.co.alphanusa.perisaipoc.data.repository

import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.core.common.DispatcherProvider
import id.co.alphanusa.perisaipoc.data.local.datasource.SettingsLocalDataSource
import id.co.alphanusa.perisaipoc.data.mapper.toDomain
import id.co.alphanusa.perisaipoc.data.remote.ApiExecutor
import id.co.alphanusa.perisaipoc.data.remote.api.ApiService
import id.co.alphanusa.perisaipoc.data.remote.dto.CentrifugoTokenRequestDto
import id.co.alphanusa.perisaipoc.domain.model.AppConfig
import id.co.alphanusa.perisaipoc.domain.model.CallParticipant
import id.co.alphanusa.perisaipoc.domain.model.CallRoom
import id.co.alphanusa.perisaipoc.domain.model.MapBounds
import id.co.alphanusa.perisaipoc.domain.model.MapOverlayItem
import id.co.alphanusa.perisaipoc.domain.model.UserProfile
import id.co.alphanusa.perisaipoc.domain.repository.CallRepository
import id.co.alphanusa.perisaipoc.domain.repository.MapRepository
import id.co.alphanusa.perisaipoc.domain.repository.RealtimeRepository
import id.co.alphanusa.perisaipoc.domain.repository.SettingsRepository
import id.co.alphanusa.perisaipoc.domain.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val executor: ApiExecutor,
) : UserRepository {

    override suspend fun getProfile(): AppResult<UserProfile> {
        val result = executor.execute("Gagal mengambil profil") { api.getUserProfile() }
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success ->
                result.data.data?.toDomain()?.let { AppResult.success(it) }
                    ?: AppResult.failure("Profil pengguna kosong")
        }
    }
}

@Singleton
class MapRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val executor: ApiExecutor,
    private val settings: SettingsLocalDataSource,
    private val httpClient: OkHttpClient,
    private val dispatchers: DispatcherProvider,
) : MapRepository {

    override suspend fun getOverlay(bounds: MapBounds): AppResult<List<MapOverlayItem>> {
        val result = executor.executeDirect {
            api.getDraw(
                long1 = bounds.westLongitude,
                lat1 = bounds.northLatitude,
                long2 = bounds.eastLongitude,
                lat2 = bounds.southLatitude,
            )
        }
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success ->
                AppResult.success(result.data.data.orEmpty().map { it.toDomain() })
        }
    }

    override suspend fun loadStickerImage(iconId: String): AppResult<ByteArray> =
        withContext(dispatchers.io) {
            val base = settings.getConfig().baseUrl.trimEnd('/')
            val url = "$base/v1/sticker/$iconId/media"
            runCatching {
                httpClient.newCall(Request.Builder().url(url).get().build()).execute()
                    .use { response ->
                        if (!response.isSuccessful) error("HTTP ${response.code}")
                        response.body?.bytes() ?: error("Body kosong")
                    }
            }.fold(
                onSuccess = { AppResult.success(it) },
                onFailure = { AppResult.failure("Gagal memuat ikon: ${it.message}", cause = it) },
            )
        }
}

@Singleton
class CallRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val executor: ApiExecutor,
) : CallRepository {

    override suspend fun joinRoom(): AppResult<CallRoom> {
        val result = executor.execute("Gagal bergabung ke room") { api.joinLivekitRoom() }
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success ->
                result.data.data?.toDomain()?.let { AppResult.success(it) }
                    ?: AppResult.failure("Token room kosong")
        }
    }

    override suspend fun getParticipants(): AppResult<List<CallParticipant>> {
        val result = executor.execute("Gagal mengambil peserta") { api.getLivekitParticipants() }
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success ->
                AppResult.success(result.data.data.orEmpty().map { it.toDomain() })
        }
    }
}

@Singleton
class RealtimeRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val executor: ApiExecutor,
) : RealtimeRepository {

    override suspend fun generateConnectionToken(): AppResult<String> {
        val result = executor.execute("Gagal membuat token realtime") {
            api.generateCentrifugoToken(CentrifugoTokenRequestDto())
        }
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success ->
                result.data.data?.token?.let { AppResult.success(it) }
                    ?: AppResult.failure("Token realtime kosong")
        }
    }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val local: SettingsLocalDataSource,
) : SettingsRepository {
    override val config: StateFlow<AppConfig> = local.config
    override fun getConfig(): AppConfig = local.getConfig()
    override fun updateConfig(config: AppConfig) = local.updateConfig(config)
    override fun resetToDefaults() = local.resetToDefaults()
}
