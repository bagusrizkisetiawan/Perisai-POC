package id.co.alphanusa.perisaipoc.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menyimpan access token yang sedang aktif **di memori saja** (sengaja tidak
 * dipersistensi; yang disimpan permanen hanya refresh token).
 */
@Singleton
class SessionManager @Inject constructor() {

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    fun currentAccessToken(): String? = _accessToken.value

    fun updateAccessToken(token: String?) {
        _accessToken.value = token
    }

    fun clear() {
        _accessToken.value = null
    }
}
