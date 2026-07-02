package id.co.alphanusa.perisaipoc.data.remote.response

// Refresh Token Response
data class RefreshTokenResponse(
    val message: String,
    val data: RefreshTokenData,
)

data class RefreshTokenData(
    val accessToken: String,
)
