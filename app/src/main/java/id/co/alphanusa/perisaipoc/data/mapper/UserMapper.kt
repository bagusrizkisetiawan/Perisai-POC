package id.co.alphanusa.perisaipoc.data.mapper

import id.co.alphanusa.perisaipoc.data.remote.dto.PocDataDto
import id.co.alphanusa.perisaipoc.data.remote.dto.UserDataDto
import id.co.alphanusa.perisaipoc.domain.model.PocDevice
import id.co.alphanusa.perisaipoc.domain.model.UserProfile

fun UserDataDto.toDomain(): UserProfile = UserProfile(
    id = id.orEmpty(),
    name = name.orEmpty(),
    type = type.orEmpty(),
    serialNumber = serialNumber.orEmpty(),
    locationName = locationName.orEmpty(),
    latitude = latitude ?: 0.0,
    longitude = longitude ?: 0.0,
)

/** Mengembalikan null bila server tidak mengirim ID perangkat. */
fun PocDataDto.toDomain(): PocDevice? {
    val deviceId = id?.takeIf { it.isNotBlank() } ?: return null
    return PocDevice(
        id = deviceId,
        name = name.orEmpty(),
        serialNumber = serialNumber.orEmpty(),
        locationName = locationName.orEmpty(),
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
    )
}
