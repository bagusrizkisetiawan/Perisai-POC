package id.co.alphanusa.perisaipoc.domain.model

/**
 * Identitas perangkat POC. [id] dipakai menyusun URL RTMP tujuan streaming.
 */
data class PocDevice(
    val id: String,
    val name: String,
    val serialNumber: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
)
