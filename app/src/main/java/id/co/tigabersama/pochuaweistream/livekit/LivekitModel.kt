package id.co.tigabersama.pochuaweistream.livekit

data class LivekitResponse(
    val message: String,
    val data: LivekitData
)

data class LivekitData(
    val room_id: String,
    val token: String
)


data class ParticipantsResponse(
    val status: String,
    val message: String,
    val data: List<Participant>
)

data class Participant(
    val identity: String,
    val joined_at: Long,
    val name: String,
    val sid: String,
    val state: String
)