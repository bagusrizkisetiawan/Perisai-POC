package id.co.tigabersama.pochuaweistream.auth

import com.google.gson.annotations.SerializedName

// Login Request
data class LoginRequest(
    @SerializedName("Otp")
    val otp: String
)

// Login Response
data class LoginResponse(
    val message: String,
    val data: LoginData
)

data class LoginData(
    val accessToken: String,
    val refreshToken: String
)

// Refresh Token Request
data class RefreshTokenRequest(
    @SerializedName("RefreshToken")
    val refreshToken: String
)

// Refresh Token Response
data class RefreshTokenResponse(
    val message: String,
    val data: RefreshTokenData
)

data class RefreshTokenData(
    val accessToken: String
)

// Auth State
data class AuthState(
    val isLoggedIn: Boolean = false,
    val accessToken: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

// API Error Response
data class ApiErrorResponse(
    val message: String?,
    val error: String?
)
