package id.co.alphanusa.perisaipoc.domain.model

/** Kredensial untuk bergabung ke room LiveKit. */
data class CallRoom(
    val roomId: String,
    val token: String,
)

/** Peserta yang sedang berada di room. */
data class CallParticipant(
    val identity: String,
    val name: String,
    val sid: String,
    val state: String,
    val joinedAt: Long,
)
