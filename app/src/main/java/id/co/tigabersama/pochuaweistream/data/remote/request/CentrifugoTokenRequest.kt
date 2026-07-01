package id.co.tigabersama.pochuaweistream.data.remote.request

import com.google.gson.annotations.SerializedName

data class CentrifugoTokenRequest(
    @SerializedName("client_id") val clientId: String = "",
)
