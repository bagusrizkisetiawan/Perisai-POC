package id.co.alphanusa.perisaipoc.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    @SerializedName("Otp") val otp: String,
)

data class LoginResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: LoginDataDto?,
)

data class LoginDataDto(
    @SerializedName("accessToken") val accessToken: String?,
    @SerializedName("refreshToken") val refreshToken: String?,
)

data class RefreshTokenRequestDto(
    @SerializedName("RefreshToken") val refreshToken: String,
)

data class RefreshTokenResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: RefreshTokenDataDto?,
)

data class RefreshTokenDataDto(
    @SerializedName("accessToken") val accessToken: String?,
)

data class ApiErrorDto(
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?,
)
