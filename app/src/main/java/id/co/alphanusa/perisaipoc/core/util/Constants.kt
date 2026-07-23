package id.co.alphanusa.perisaipoc.core.util

/**
 * Nilai tetap aplikasi. Dikumpulkan di satu tempat agar tidak tersebar sebagai
 * "magic number" / string keras di seluruh kode.
 */
object Constants {

    /** Batas waktu operasi autentikasi (login & refresh token). */
    const val AUTH_TIMEOUT_MS = 5_000L

    /** Jeda antar pengiriman telemetri ke Centrifugo. */
    const val TELEMETRY_INTERVAL_MS = 100L

    /** Lama menahan tombol Stop sebelum streaming benar-benar dihentikan. */
    const val HOLD_TO_STOP_MS = 3_000

    /** Lama tanpa sentuhan sebelum peta kembali mengikuti lokasi perangkat. */
    const val MAP_RECENTER_IDLE_MS = 4_000L

    /** Lama layar splash ditampilkan. */
    const val SPLASH_DURATION_MS = 2_000L

    /** Jeda polling persentase baterai. */
    const val BATTERY_POLL_INTERVAL_MS = 2_000L

    /** Jumlah bagian pada QR login: baseUrl|centrifugo|rtmp|livekit|token. */
    const val QR_PART_COUNT = 5

    /** Path yang ditambahkan ke host Centrifugo dari QR. */
    const val CENTRIFUGO_WS_PATH = "/connection/websocket"

    /** Prefix channel telemetri; channel akhir = "$TELEMETRY_CHANNEL_PREFIX<pocId>". */
    const val TELEMETRY_CHANNEL_PREFIX = "mobile-data:"

    /** Nilai wire pada URL RTMP — dikunci oleh server, jangan diubah. */
    const val RTMP_USER_PARAM = "drone"

    object Stream {
        // 4:3 = rasio native sensor kamera -> preview & stream tanpa crop/letterbox.
        const val WIDTH = 1440
        const val HEIGHT = 1080
        const val FPS = 30
        const val VIDEO_BITRATE = 4_000_000
        const val AUDIO_BITRATE = 128 * 1024
        const val SAMPLE_RATE = 44_100
        const val I_FRAME_INTERVAL_SEC = 2

        /** Rasio preview pada orientasi portrait (4:3 diputar). */
        const val PREVIEW_ASPECT_RATIO = 3f / 4f
    }

    object Gallery {
        const val VIDEO_ALBUM = "PERISAI POC VIDEO"
        const val PHOTO_ALBUM = "PERISAI POC Photo"
        const val JPEG_QUALITY = 95
        const val FILE_TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss"
    }

    object Prefs {
        const val SETTINGS_NAME = "app_settings"
        const val AUTH_NAME = "auth_prefs"

        const val KEY_BASE_URL = "base_url"
        const val KEY_CENTRIFUGO_WS_URL = "centrifugo_websocket_url"
        const val KEY_RTMP_URL = "rtmp_url"
        const val KEY_LIVEKIT_URL = "livekit_url"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }

    object Map {
        const val DEFAULT_LATITUDE = -6.9828
        const val DEFAULT_LONGITUDE = 110.4091
        const val DEFAULT_ZOOM = 19.0
        const val BOUNDS_DEBOUNCE_MS = 500L
        const val FETCH_DEBOUNCE_MS = 400L
    }
}
