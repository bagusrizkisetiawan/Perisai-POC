package id.co.tigabersama.pochuaweistream.auth

import android.content.Context
import android.util.Log
import id.co.tigabersama.pochuaweistream.BuildConfig
import id.co.tigabersama.pochuaweistream.ui.screen.setting.AppSettingsManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AuthManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AuthManager"
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val secureStorage = SecureTokenStorage(context)
    private lateinit var _authRepository: AuthRepository
    private val settingsManager = AppSettingsManager.getInstance(context)

    // Get base URL from settings (which falls back to secrets.properties if not set)
    private val baseUrl = settingsManager.getBaseUrl().let { url ->
        if (!url.endsWith("/")) "$url/" else url
    }

    private var currentAccessToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val accessToken = getCurrentAccessToken()

        val newRequest = if (accessToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        chain.proceed(newRequest)
    }

    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            // If we get 401, try to refresh the token
            if (response.code == 401) {
                Log.d(TAG, "Got 401, attempting token refresh")

                val newToken = runBlocking {
                    try {
                        _authRepository.refreshToken().getOrNull()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during token refresh", e)
                        null
                    }
                }

                return if (newToken != null) {
                    Log.d(TAG, "Token refresh successful, retrying request")
                    setCurrentAccessToken(newToken)
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                } else {
                    Log.d(TAG, "Token refresh failed, forcing logout")
                    // Refresh failed, logout user
                    _authRepository.logout()
                    null
                }
            }
            return null
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .apply {
            if (BuildConfig.DEBUG) {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                addInterceptor(loggingInterceptor)
            }
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(AuthApiService::class.java)

    init {
        _authRepository = AuthRepository(apiService, secureStorage, context)
    }

    val authRepository: AuthRepository
        get() = _authRepository

    fun setCurrentAccessToken(token: String?) {
        currentAccessToken = token
    }

    fun getCurrentAccessToken(): String? = currentAccessToken

    fun getHttpClient(): OkHttpClient = httpClient

    fun getRetrofit(): Retrofit = retrofit

    fun clearSession() {
        currentAccessToken = null
        secureStorage.clearAll()
    }

    fun recreate(context: Context) {
        INSTANCE = AuthManager(context.applicationContext)
    }
}
