package id.co.alphanusa.perisaipoc.domain.usecase

import id.co.alphanusa.perisaipoc.core.util.Constants
import id.co.alphanusa.perisaipoc.domain.model.AppConfig
import id.co.alphanusa.perisaipoc.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Membaca QR login dan menyimpan konfigurasi endpoint yang terkandung di dalamnya.
 *
 * Format QR: `baseUrl|centrifugoHost|rtmpUrl|livekitUrl|otp` — dipisah tanda `|`.
 * Mengembalikan OTP untuk dipakai proses login, atau null bila format tidak sesuai.
 */
class ApplyQrConfigUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    operator fun invoke(rawQr: String): String? {
        val parts = rawQr.split(SEPARATOR)
        if (parts.size != Constants.QR_PART_COUNT) return null

        val (baseUrl, centrifugoHost, rtmpUrl, livekitUrl) = parts
        val otp = parts[INDEX_OTP]

        settingsRepository.updateConfig(
            AppConfig(
                baseUrl = baseUrl,
                centrifugoWebSocketUrl = centrifugoHost + Constants.CENTRIFUGO_WS_PATH,
                rtmpUrl = rtmpUrl,
                livekitUrl = livekitUrl,
            ),
        )
        return otp
    }

    private companion object {
        const val SEPARATOR = "|"
        const val INDEX_OTP = 4
    }
}
