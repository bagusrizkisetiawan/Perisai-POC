package id.co.alphanusa.perisaipoc.data.remote.interceptor

import id.co.alphanusa.perisaipoc.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/** Menambahkan header `Authorization: Bearer <accessToken>` bila sesi tersedia. */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = sessionManager.currentAccessToken() ?: return chain.proceed(request)

        val authorized = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authorized)
    }
}
