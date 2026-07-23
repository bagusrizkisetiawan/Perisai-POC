package id.co.alphanusa.perisaipoc.domain.model

/** Titik koordinat dalam domain. */
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
)

/** Lampiran opsional pada sebuah item peta. */
data class MapAttachment(
    val url: String?,
    val name: String?,
)

/** Jenis gambar overlay yang didukung server. */
enum class MapOverlayType(val wireValue: String) {
    PIN("pin"),
    LINE("line"),
    AREA("area"),
    CIRCLE("circle"),
    UNKNOWN(""),
    ;

    companion object {
        fun fromWire(value: String?): MapOverlayType =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

/** Satu objek gambar di peta (pin, garis, area, atau lingkaran). */
data class MapOverlayItem(
    val id: String,
    val type: MapOverlayType,
    val name: String?,
    val color: String?,
    val point: GeoLocation?,
    val points: List<GeoLocation>,
    val radius: Double,
    val size: Double,
    val iconId: String?,
    val notes: List<String>,
    val attachment: MapAttachment?,
)

/** Batas area peta yang sedang dilihat, dipakai untuk memuat overlay. */
data class MapBounds(
    val westLongitude: Double,
    val northLatitude: Double,
    val eastLongitude: Double,
    val southLatitude: Double,
)
