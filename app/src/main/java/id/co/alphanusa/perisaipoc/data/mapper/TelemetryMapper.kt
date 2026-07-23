package id.co.alphanusa.perisaipoc.data.mapper

import id.co.alphanusa.perisaipoc.data.remote.dto.BatteryDto
import id.co.alphanusa.perisaipoc.data.remote.dto.TelemetryDto
import id.co.alphanusa.perisaipoc.domain.model.BatteryInfo
import id.co.alphanusa.perisaipoc.domain.model.PocTelemetry

fun BatteryInfo.toDto(): BatteryDto = BatteryDto(
    percentage = percentage,
    voltage = voltage,
    status = status.name,
)

/** Domain → bentuk kabel yang dipublish ke Centrifugo. */
fun PocTelemetry.toDto(): TelemetryDto = TelemetryDto(
    pitch = pitch,
    roll = roll,
    yaw = yaw,
    battery = battery?.toDto(),
    aircraftLatitude = latitude,
    aircraftLongitude = longitude,
    aircraftAltitude = altitude,
    homeLatitude = homeLatitude,
    homeLongitude = homeLongitude,
    gpsSatelliteCount = gpsSatelliteCount,
    gpsSignalLevel = gpsSignalLevel.wireValue,
    timestamp = timestamp,
)
