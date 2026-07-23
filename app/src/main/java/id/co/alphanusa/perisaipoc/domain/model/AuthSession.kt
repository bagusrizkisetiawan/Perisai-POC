package id.co.alphanusa.perisaipoc.domain.model

/** Sesi autentikasi hasil login / refresh token. */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String? = null,
)
