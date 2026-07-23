package id.co.alphanusa.perisaipoc.data.mapper

import id.co.alphanusa.perisaipoc.data.remote.dto.LoginDataDto
import id.co.alphanusa.perisaipoc.domain.model.AuthSession

fun LoginDataDto.toDomain(): AuthSession? {
    val access = accessToken ?: return null
    return AuthSession(accessToken = access, refreshToken = refreshToken)
}
