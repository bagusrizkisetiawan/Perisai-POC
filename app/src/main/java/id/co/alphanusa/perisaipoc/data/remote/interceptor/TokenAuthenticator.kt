package id.co.alphanusa.perisaipoc.data.remote.interceptor

import android.util.Log
import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.data.local.SessionManager
import id.co.alphanusa.perisaipoc.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Saat server membalas 401, mencoba memperbarui access token sekali lalu
 * mengulang request. Bila pembaruan gagal, sesi dibersihkan.
 *
 * [AuthRepository] di-inject lewat [Provider] karena repository itu sendiri
 * memakai OkHttp yang sedang dibangun — menghindari ketergantungan melingkar.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val authRepository: Provider<AuthRepository>,
    private val sessionManager: SessionManager,
) : Authenticator {

    private companion object {
        const val TAG = "TokenAuthenticator"
        const val HEADER_AUTHORIZATION = "Authorization"
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != HTTP_UNAUTHORIZED) return null
        // Sudah pernah dicoba ulang → jangan berulang tanpa henti.
        if (response.request.header(HEADER_AUTHORIZATION) != null && response.priorResponse != null) {
            return null
        }

        Log.d(TAG, "Menerima 401, mencoba memperbarui token")
        val refreshed = runBlocking { authRepository.get().refreshSession() }

        return when (refreshed) {
            is AppResult.Success -> {
                val newToken = refreshed.data.accessToken
                sessionManager.updateAccessToken(newToken)
                response.request.newBuilder()
                    .header(HEADER_AUTHORIZATION, "Bearer $newToken")
                    .build()
            }

            is AppResult.Failure -> {
                Log.d(TAG, "Pembaruan token gagal: ${refreshed.message}")
                sessionManager.clear()
                null
            }
        }
    }
}

private const val HTTP_UNAUTHORIZED = 401
