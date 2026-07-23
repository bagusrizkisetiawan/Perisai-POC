package id.co.alphanusa.perisaipoc.data.mapper

import id.co.alphanusa.perisaipoc.data.remote.dto.LivekitDataDto
import id.co.alphanusa.perisaipoc.data.remote.dto.ParticipantDto
import id.co.alphanusa.perisaipoc.domain.model.CallParticipant
import id.co.alphanusa.perisaipoc.domain.model.CallRoom

fun LivekitDataDto.toDomain(): CallRoom? {
    val roomToken = token?.takeIf { it.isNotBlank() } ?: return null
    return CallRoom(roomId = roomId.orEmpty(), token = roomToken)
}

fun ParticipantDto.toDomain(): CallParticipant = CallParticipant(
    identity = identity.orEmpty(),
    name = name.orEmpty(),
    sid = sid.orEmpty(),
    state = state.orEmpty(),
    joinedAt = joinedAt,
)
