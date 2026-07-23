package id.co.alphanusa.perisaipoc.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.core.util.Constants
import id.co.alphanusa.perisaipoc.domain.usecase.ApplyQrConfigUseCase
import id.co.alphanusa.perisaipoc.domain.usecase.HasStoredSessionUseCase
import id.co.alphanusa.perisaipoc.domain.usecase.LoginWithOtpUseCase
import id.co.alphanusa.perisaipoc.domain.usecase.LogoutUseCase
import id.co.alphanusa.perisaipoc.domain.usecase.RefreshSessionUseCase
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/** Status autentikasi yang dibaca UI. */
data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** True bila sesi masih tersimpan tetapi gagal terhubung (tawarkan Reconnect). */
    val isConnectionError: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val applyQrConfig: ApplyQrConfigUseCase,
    private val loginWithOtp: LoginWithOtpUseCase,
    private val refreshSession: RefreshSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val hasStoredSession: HasStoredSessionUseCase,
) : ViewModel() {

    private companion object {
        const val TAG = "AuthViewModel"
        const val INVALID_QR = "Format QR tidak valid!"
        const val LOGIN_TIMEOUT = "Waktu login habis (Timeout). Periksa koneksi server/internet."
        const val REFRESH_TIMEOUT =
            "Server tidak merespons (timeout 5 detik). Periksa koneksi lalu coba Reconnect."
        const val GENERIC_ERROR = "Terjadi kesalahan sistem/jaringan."
    }

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        if (hasStoredSession()) reconnect() else _state.value = AuthUiState()
    }

    /** Dipanggil setelah QR berhasil dipindai. */
    fun onQrScanned(rawQr: String) {
        val otp = applyQrConfig(rawQr)
        if (otp == null) {
            _state.value = _state.value.copy(error = INVALID_QR)
            return
        }
        login(otp)
    }

    private fun login(otp: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runWithTimeout(
                timeoutMessage = LOGIN_TIMEOUT,
                markConnectionError = false,
            ) {
                when (val result = loginWithOtp(otp)) {
                    is AppResult.Success -> _state.value = AuthUiState(isLoggedIn = true)
                    is AppResult.Failure -> {
                        Log.e(TAG, "Login gagal: ${result.message}")
                        _state.value = AuthUiState(error = result.message)
                    }
                }
            }
        }
    }

    /** Mencoba memakai kembali sesi tersimpan; tidak menghapus sesi bila gagal. */
    fun reconnect() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runWithTimeout(
                timeoutMessage = REFRESH_TIMEOUT,
                markConnectionError = true,
            ) {
                when (val result = refreshSession()) {
                    is AppResult.Success -> _state.value = AuthUiState(isLoggedIn = true)
                    is AppResult.Failure -> {
                        Log.e(TAG, "Refresh gagal: ${result.message}")
                        _state.value = AuthUiState(
                            error = result.message,
                            isConnectionError = true,
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        logoutUseCase()
        _state.value = AuthUiState()
        Log.d(TAG, "Sesi dibersihkan")
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private suspend fun runWithTimeout(
        timeoutMessage: String,
        markConnectionError: Boolean,
        block: suspend () -> Unit,
    ) {
        try {
            withTimeout(Constants.AUTH_TIMEOUT_MS) { block() }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Operasi melewati batas waktu", e)
            _state.value = AuthUiState(
                error = timeoutMessage,
                isConnectionError = markConnectionError,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Kesalahan tak terduga", e)
            _state.value = AuthUiState(
                error = e.message ?: GENERIC_ERROR,
                isConnectionError = markConnectionError,
            )
        }
    }
}
