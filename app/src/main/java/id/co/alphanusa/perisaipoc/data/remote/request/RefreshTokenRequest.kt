package id.co.alphanusa.perisaipoc.data.remote.request

import com.google.gson.annotations.SerializedName

// Refresh Token Request
data class RefreshTokenRequest(
    @SerializedName("RefreshToken")
    val refreshToken: String,
)
