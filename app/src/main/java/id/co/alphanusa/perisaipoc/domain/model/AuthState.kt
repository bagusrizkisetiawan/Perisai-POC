package id.co.alphanusa.perisaipoc.domain.model

// Auth State
data class AuthState(
    val isLoggedIn: Boolean = false,
    val accessToken: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // true bila sesi masih ada (refresh token tersimpan) tapi gagal connect (timeout/network).
    // Dipakai UI untuk menampilkan tombol Reconnect + Logout, bukan Scan QR.
    val isConnectionError: Boolean = false,
)

// API Error Response
data class ApiErrorResponse(
    val message: String?,
    val error: String?,
)
