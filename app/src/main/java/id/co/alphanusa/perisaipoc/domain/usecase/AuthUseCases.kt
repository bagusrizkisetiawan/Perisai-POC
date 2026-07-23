package id.co.alphanusa.perisaipoc.domain.usecase

import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.domain.model.AuthSession
import id.co.alphanusa.perisaipoc.domain.repository.AuthRepository
import javax.inject.Inject

/** Login memakai OTP hasil scan QR. */
class LoginWithOtpUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(otp: String): AppResult<AuthSession> = repository.login(otp)
}

/** Memperbarui access token dari refresh token tersimpan. */
class RefreshSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): AppResult<AuthSession> = repository.refreshSession()
}

/** Mengakhiri sesi dan menghapus token tersimpan. */
class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke() = repository.logout()
}

/** Mengecek apakah masih ada sesi tersimpan saat aplikasi dibuka. */
class HasStoredSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Boolean = repository.hasStoredSession()
}
