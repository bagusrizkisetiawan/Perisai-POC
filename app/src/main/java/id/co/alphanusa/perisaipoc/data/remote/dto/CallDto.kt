package id.co.alphanusa.perisaipoc.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LivekitResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: LivekitDataDto?,
)

data class LivekitDataDto(
    @SerializedName("room_id") val roomId: String?,
    @SerializedName("token") val token: String?,
)

data class ParticipantsResponseDto(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<ParticipantDto>?,
)

data class ParticipantDto(
    @SerializedName("identity") val identity: String?,
    @SerializedName("joined_at") val joinedAt: Long = 0L,
    @SerializedName("name") val name: String?,
    @SerializedName("sid") val sid: String?,
    @SerializedName("state") val state: String?,
)
