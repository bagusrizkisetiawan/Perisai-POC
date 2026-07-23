package id.co.alphanusa.perisaipoc.data.remote.interceptor

import id.co.alphanusa.perisaipoc.data.local.datasource.SettingsLocalDataSource
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mengarahkan setiap request ke base URL terbaru dari pengaturan.
 *
 * Base URL aplikasi bisa berubah saat runtime (diisi dari QR login), sedangkan
 * Retrofit hanya dibangun sekali. Interceptor ini menulis ulang skema/host/port
 * (beserta prefix path base URL) pada saat request dikirim, sehingga tidak perlu
 * membangun ulang Retrofit ketika pengaturan berubah.
 */
@Singleton
class BaseUrlInterceptor @Inject constructor(
    private val settings: SettingsLocalDataSource,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val base = settings.getConfig().baseUrl.toHttpUrlOrNull()
            ?: return chain.proceed(request)

        val rewritten = request.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .encodedPath(base.prefixedPath(request.url))
            .build()

        return chain.proceed(request.newBuilder().url(rewritten).build())
    }

    /** Menggabungkan prefix path base URL (bila ada) dengan path endpoint. */
    private fun HttpUrl.prefixedPath(requestUrl: HttpUrl): String {
        val prefix = encodedPath.trimEnd('/')
        return if (prefix.isEmpty()) requestUrl.encodedPath else prefix + requestUrl.encodedPath
    }
}
