package id.co.alphanusa.perisaipoc.data.mapper

import id.co.alphanusa.perisaipoc.data.remote.dto.DrawAttachmentDto
import id.co.alphanusa.perisaipoc.data.remote.dto.DrawItemDto
import id.co.alphanusa.perisaipoc.data.remote.dto.LatLongDto
import id.co.alphanusa.perisaipoc.domain.model.GeoLocation
import id.co.alphanusa.perisaipoc.domain.model.MapAttachment
import id.co.alphanusa.perisaipoc.domain.model.MapOverlayItem
import id.co.alphanusa.perisaipoc.domain.model.MapOverlayType

fun LatLongDto.toDomain(): GeoLocation = GeoLocation(latitude = lat, longitude = long)

fun DrawAttachmentDto.toDomain(): MapAttachment = MapAttachment(url = url, name = name)

fun DrawItemDto.toDomain(): MapOverlayItem = MapOverlayItem(
    id = id.orEmpty(),
    type = MapOverlayType.fromWire(type),
    name = name,
    color = color,
    point = point?.toDomain(),
    points = points?.map { it.toDomain() }.orEmpty(),
    radius = radius,
    size = size,
    iconId = icon?.takeIf { it.isNotBlank() },
    notes = notes.orEmpty(),
    attachment = attachment?.toDomain(),
)
