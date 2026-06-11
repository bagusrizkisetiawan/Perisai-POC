package id.co.tigabersama.pochuaweistream.data.remote.request

import com.google.gson.annotations.SerializedName

// Login Request
data class LoginRequest(
    @SerializedName("Otp")
    val otp: String
)
