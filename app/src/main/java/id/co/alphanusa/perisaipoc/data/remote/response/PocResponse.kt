package id.co.alphanusa.perisaipoc.data.remote.response

data class PocInfoData(
    val ID: String,
    val name: String,
    val serialNumber: String,
    val key: String,
    val agencyId: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
)

data class PocResponse(
    val message: String,
    val data: PocInfoData,
)
