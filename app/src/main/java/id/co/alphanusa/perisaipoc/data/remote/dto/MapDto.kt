package id.co.alphanusa.perisaipoc.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DrawResponseDto(
    @SerializedName("data") val data: List<DrawItemDto>? = null,
    @SerializedName("message") val message: String? = null,
)

data class DrawItemDto(
    @SerializedName("ID") val id: String?,
    @SerializedName("CreatedAt") val createdAt: String?,
    @SerializedName("UpdatedAt") val updatedAt: String?,
    @SerializedName("DeletedAt") val deletedAt: String?,
    @SerializedName("Type") val type: String?,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("Color") val color: String? = null,
    @SerializedName("Point") val point: LatLongDto? = null,
    @SerializedName("Points") val points: List<LatLongDto>? = null,
    @SerializedName("Radius") val radius: Double = 0.0,
    @SerializedName("Size") val size: Double = 0.0,
    @SerializedName("Icon") val icon: String? = null,
    @SerializedName("Notes") val notes: List<String>? = null,
    @SerializedName("Attachment") val attachment: DrawAttachmentDto? = null,
)

data class LatLongDto(
    @SerializedName(
        value = "Lat",
        alternate = ["lat", "Latitude", "latitude"],
    )
    val lat: Double,
    @SerializedName(
        value = "Long",
        alternate = ["long", "lng", "Lng", "Longitude", "longitude", "Lon", "lon"],
    )
    val long: Double,
)

data class DrawAttachmentDto(
    @SerializedName("Url") val url: String? = null,
    @SerializedName("Name") val name: String? = null,
)
