package id.co.alphanusa.perisaipoc.data.repository

import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.core.util.Constants
import id.co.alphanusa.perisaipoc.data.local.SessionManager
import id.co.alphanusa.perisaipoc.data.local.datasource.SettingsLocalDataSource
import id.co.alphanusa.perisaipoc.data.mapper.toDomain
import id.co.alphanusa.perisaipoc.data.remote.ApiExecutor
import id.co.alphanusa.perisaipoc.data.remote.api.ApiService
import id.co.alphanusa.perisaipoc.domain.model.PocDevice
import id.co.alphanusa.perisaipoc.domain.repository.PocRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PocRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val executor: ApiExecutor,
    private val settings: SettingsLocalDataSource,
    private val sessionManager: SessionManager,
) : PocRepository {

    private companion object {
        const val DEVICE_FALLBACK = "Gagal mengambil data perangkat dari server"
        const val EMPTY_DEVICE_ID = "Data ID perangkat kosong"
        const val NO_ACCESS_TOKEN = "Sesi belum siap"
    }

    override suspend fun getDevice(): AppResult<PocDevice> {
        val result = executor.execute(DEVICE_FALLBACK) { api.getPocDevice() }
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success ->
                result.data.data?.toDomain()?.let { AppResult.success(it) }
                    ?: AppResult.failure(EMPTY_DEVICE_ID)
        }
    }

    /**
     * Format URL dikunci oleh server:
     * `<baseRtmp>/<deviceId>?user=drone&pass=<accessToken>`.
     */
    override suspend fun buildStreamUrl(): AppResult<String> {
        val device = when (val result = getDevice()) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        val accessToken = sessionManager.currentAccessToken()
            ?: return AppResult.failure(NO_ACCESS_TOKEN)

        val baseRtmp = settings.getConfig().rtmpUrl.trimEnd('/')
        val url = "$baseRtmp/${device.id}" +
            "?user=${Constants.RTMP_USER_PARAM}&pass=$accessToken"
        return AppResult.success(url)
    }
}
