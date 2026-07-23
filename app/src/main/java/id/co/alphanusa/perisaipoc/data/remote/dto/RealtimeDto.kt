package id.co.alphanusa.perisaipoc.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CentrifugoTokenRequestDto(
    @SerializedName("client_id") val clientId: String = "",
)

data class CentrifugoTokenResponseDto(
    @SerializedName("data") val data: CentrifugoTokenDataDto?,
    @SerializedName("message") val message: String?,
)

data class CentrifugoTokenDataDto(
    @SerializedName("token") val token: String?,
)

/**
 * Bentuk kabel telemetri yang dipublish ke channel Centrifugo.
 * Nama field WAJIB sama persis dengan yang dibaca server.
 */
data class TelemetryDto(
    @SerializedName("pitch") val pitch: Double,
    @SerializedName("roll") val roll: Double,
    @SerializedName("yaw") val yaw: Double,
    @SerializedName("battery") val battery: BatteryDto?,
    @SerializedName("aircraft_latitude") val aircraftLatitude: Double,
    @SerializedName("aircraft_longitude") val aircraftLongitude: Double,
    @SerializedName("aircraft_altitude") val aircraftAltitude: Double,
    @SerializedName("home_latitude") val homeLatitude: Double,
    @SerializedName("home_longitude") val homeLongitude: Double,
    @SerializedName("gps_satellite_count") val gpsSatelliteCount: Int,
    @SerializedName("gps_signal_level") val gpsSignalLevel: String,
    @SerializedName("timestamp") val timestamp: Long,
)

data class BatteryDto(
    @SerializedName("battery_percentage") val percentage: Int,
    @SerializedName("battery_voltage") val voltage: Float,
    @SerializedName("battery_status") val status: String,
)
