package id.co.alphanusa.perisaipoc.data.remote.response

data class LivekitResponse(
    val message: String,
    val data: LivekitData,
)

data class LivekitData(
    val room_id: String,
    val token: String,
)
