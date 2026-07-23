package id.co.alphanusa.perisaipoc.domain.model

/**
 * Konfigurasi endpoint aplikasi. Diisi dari QR saat login dan dapat diubah
 * manual lewat layar Settings.
 */
data class AppConfig(
    val baseUrl: String,
    val centrifugoWebSocketUrl: String,
    val rtmpUrl: String,
    val livekitUrl: String,
)
