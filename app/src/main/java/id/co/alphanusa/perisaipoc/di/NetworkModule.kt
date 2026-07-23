package id.co.alphanusa.perisaipoc.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.co.alphanusa.perisaipoc.BuildConfig
import id.co.alphanusa.perisaipoc.data.remote.api.ApiService
import id.co.alphanusa.perisaipoc.data.remote.interceptor.AuthInterceptor
import id.co.alphanusa.perisaipoc.data.remote.interceptor.BaseUrlInterceptor
import id.co.alphanusa.perisaipoc.data.remote.interceptor.TokenAuthenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 30L

    /**
     * Base URL sebenarnya ditentukan saat runtime oleh [BaseUrlInterceptor];
     * nilai ini hanya placeholder agar Retrofit dapat dibangun.
     */
    private const val PLACEHOLDER_BASE_URL = "http://localhost/"

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        baseUrlInterceptor: BaseUrlInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(baseUrlInterceptor)
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    },
                )
            }
        }
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit = Retrofit.Builder()
        .baseUrl(startingBaseUrl())
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    private fun startingBaseUrl(): String {
        val configured = BuildConfig.BASE_URL
        if (configured.isBlank()) return PLACEHOLDER_BASE_URL
        return if (configured.endsWith("/")) configured else "$configured/"
    }
}
