package id.co.alphanusa.perisaipoc.core.util

import android.util.Base64
import com.google.gson.Gson

/**
 * Membaca payload JWT tanpa memverifikasi tanda tangan.
 *
 * Dipakai untuk mengambil klaim `sub` dari token Centrifugo — nilai itulah yang
 * menjadi id perangkat penyusun nama channel telemetri.
 */
object JwtDecoder {

    private const val JWT_SEGMENT_COUNT = 3
    private const val PAYLOAD_INDEX = 1
    private val gson = Gson()

    /** Mengembalikan klaim `sub`, atau null bila token tidak berbentuk JWT. */
    fun extractSubject(token: String): String? = runCatching {
        val parts = token.split(".")
        if (parts.size != JWT_SEGMENT_COUNT) return null

        val flags = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        val payload = Base64.decode(parts[PAYLOAD_INDEX], flags)
        val claims = gson.fromJson(String(payload, Charsets.UTF_8), Map::class.java)
        claims["sub"] as? String
    }.getOrNull()
}
