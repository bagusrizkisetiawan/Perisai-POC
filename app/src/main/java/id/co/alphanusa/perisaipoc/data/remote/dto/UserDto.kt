package id.co.alphanusa.perisaipoc.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: UserDataDto?,
)

data class UserDataDto(
    @SerializedName("ID") val id: String? = null,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("Type") val type: String? = null,
    @SerializedName("SerialNumber") val serialNumber: String? = null,
    @SerializedName("LocationName") val locationName: String? = null,
    @SerializedName("Latitude") val latitude: Double? = null,
    @SerializedName("Longitude") val longitude: Double? = null,
    @SerializedName("CreatedAt") val createdAt: String? = null,
    @SerializedName("UpdatedAt") val updatedAt: String? = null,
    @SerializedName("DeletedAt") val deletedAt: String? = null,
)
