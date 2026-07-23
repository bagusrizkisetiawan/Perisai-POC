package id.co.alphanusa.perisaipoc.domain.model

/** Profil pengguna/perangkat yang sedang login, dipakai untuk tampilan. */
data class UserProfile(
    val id: String,
    val name: String,
    val type: String,
    val serialNumber: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
)
