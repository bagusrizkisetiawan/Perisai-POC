package id.co.alphanusa.perisaipoc.domain.repository

import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.domain.model.AuthSession
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    /** Access token aktif; null bila belum/again tidak login. */
    val accessToken: StateFlow<String?>

    /** Login memakai OTP dari QR. */
    suspend fun login(otp: String): AppResult<AuthSession>

    /** Memperbarui access token memakai refresh token tersimpan. */
    suspend fun refreshSession(): AppResult<AuthSession>

    /** Menghapus sesi (token memori + refresh token tersimpan). */
    fun logout()

    /** True bila masih ada refresh token tersimpan. */
    fun hasStoredSession(): Boolean
}
