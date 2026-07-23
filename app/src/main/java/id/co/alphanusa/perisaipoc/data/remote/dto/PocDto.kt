package id.co.alphanusa.perisaipoc.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PocResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: PocDataDto?,
)

data class PocDataDto(
    @SerializedName("ID") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("serialNumber") val serialNumber: String? = null,
    @SerializedName("key") val key: String? = null,
    @SerializedName("agencyId") val agencyId: String? = null,
    @SerializedName("locationName") val locationName: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
)
