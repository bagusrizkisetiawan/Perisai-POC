package id.co.alphanusa.perisaipoc.data.repository

import android.util.Log
import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.data.local.SessionManager
import id.co.alphanusa.perisaipoc.data.local.datasource.TokenLocalDataSource
import id.co.alphanusa.perisaipoc.data.mapper.toDomain
import id.co.alphanusa.perisaipoc.data.remote.ApiExecutor
import id.co.alphanusa.perisaipoc.data.remote.api.ApiService
import id.co.alphanusa.perisaipoc.data.remote.dto.LoginRequestDto
import id.co.alphanusa.perisaipoc.data.remote.dto.RefreshTokenRequestDto
import id.co.alphanusa.perisaipoc.domain.model.AuthSession
import id.co.alphanusa.perisaipoc.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val executor: ApiExecutor,
    private val tokenStorage: TokenLocalDataSource,
    private val sessionManager: SessionManager,
) : AuthRepository {

    private companion object {
        const val TAG = "AuthRepository"
        const val LOGIN_FALLBACK = "Login failed"
        const val REFRESH_FALLBACK = "Token refresh failed"
        const val NO_REFRESH_TOKEN = "No refresh token available"
    }

    override val accessToken: StateFlow<String?> = sessionManager.accessToken

    override suspend fun login(otp: String): AppResult<AuthSession> {
        val result = executor.execute(LOGIN_FALLBACK) { api.login(LoginRequestDto(otp)) }

        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val session = result.data.data?.toDomain()
                    ?: return AppResult.failure(LOGIN_FALLBACK)

                session.refreshToken?.let(tokenStorage::saveRefreshToken)
                sessionManager.updateAccessToken(session.accessToken)
                Log.d(TAG, "Login berhasil, token tersimpan")
                AppResult.success(session)
            }
        }
    }

    override suspend fun refreshSession(): AppResult<AuthSession> {
        val refreshToken = tokenStorage.getRefreshToken()
            ?: return AppResult.failure(NO_REFRESH_TOKEN)

        val result = executor.execute(REFRESH_FALLBACK) {
            api.refreshToken(RefreshTokenRequestDto(refreshToken))
        }

        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val newToken = result.data.data?.accessToken
                    ?: return AppResult.failure(REFRESH_FALLBACK)

                sessionManager.updateAccessToken(newToken)
                AppResult.success(AuthSession(accessToken = newToken, refreshToken = refreshToken))
            }
        }
    }

    override fun logout() {
        Log.d(TAG, "Logout, membersihkan token")
        tokenStorage.clearAll()
        sessionManager.clear()
    }

    override fun hasStoredSession(): Boolean = tokenStorage.hasRefreshToken()
}
