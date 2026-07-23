package id.co.alphanusa.perisaipoc.domain.model

/**
 * Telemetri perangkat yang dikirim berkala ke Centrifugo.
 * Model murni tanpa anotasi serialisasi — bentuk kabelnya ada di `data/remote/dto`.
 */
data class PocTelemetry(
    val pitch: Double,
    val roll: Double,
    val yaw: Double,
    val battery: BatteryInfo?,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val homeLatitude: Double,
    val homeLongitude: Double,
    val gpsSatelliteCount: Int,
    val gpsSignalLevel: GpsSignalLevel,
    val timestamp: Long,
)

data class BatteryInfo(
    val percentage: Int,
    val voltage: Float,
    val status: BatteryStatus,
)

enum class GpsSignalLevel(val wireValue: String) {
    GOOD("GOOD"),
    NO_GPS("NO_GPS"),
}

enum class BatteryStatus(val index: Int) {
    /** Baterai normal. */
    NORMAL(0),

    /** Mulai lemah — sebaiknya kembali ke titik aman. */
    WARNING_LEVEL_1(1),

    /** Sangat lemah — harus segera berhenti. */
    WARNING_LEVEL_2(2),

    /** Pembacaan baterai bermasalah. */
    ERROR(3),

    /** Suhu baterai terlalu tinggi. */
    OVERHEATING(4),

    /** Belum diketahui / masih inisialisasi. */
    UNKNOWN(5),
    ;

    companion object {
        private const val NORMAL_MIN = 50
        private const val WARNING_1_MIN = 20
        private const val WARNING_2_MIN = 1

        /** Menentukan status dari persentase baterai. */
        fun fromPercentage(percentage: Int): BatteryStatus = when {
            percentage >= NORMAL_MIN -> NORMAL
            percentage >= WARNING_1_MIN -> WARNING_LEVEL_1
            percentage >= WARNING_2_MIN -> WARNING_LEVEL_2
            else -> ERROR
        }
    }
}
