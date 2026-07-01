package id.co.tigabersama.pochuaweistream.data.remote.api

import id.co.tigabersama.pochuaweistream.data.remote.request.CentrifugoTokenRequest
import id.co.tigabersama.pochuaweistream.data.remote.request.LoginRequest
import id.co.tigabersama.pochuaweistream.data.remote.request.RefreshTokenRequest
import id.co.tigabersama.pochuaweistream.data.remote.response.CentrifugoTokenResponse
import id.co.tigabersama.pochuaweistream.data.remote.response.DrawResponse
import id.co.tigabersama.pochuaweistream.data.remote.response.LivekitResponse
import id.co.tigabersama.pochuaweistream.data.remote.response.LoginResponse
import id.co.tigabersama.pochuaweistream.data.remote.response.ParticipantsResponse
import id.co.tigabersama.pochuaweistream.data.remote.response.PocResponse
import id.co.tigabersama.pochuaweistream.data.remote.response.RefreshTokenResponse
import id.co.tigabersama.pochuaweistream.data.remote.response.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Single consolidated Retrofit service holding every endpoint of the app.
 * (Sebelumnya tersebar di AuthApiService, UserApiService, DrawApiService,
 * LivekitApiService, CentrifugoApiService, DroneApiService.)
 */
interface ApiService {

    // ---- Auth ----
    @POST("v1/mobile/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("v1/mobile/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>

    // ---- Centrifugo ----
    @POST("v1/mobile/auth/gentoken-centrifugo")
    suspend fun generateCentrifugoToken(
        @Body request: CentrifugoTokenRequest,
    ): Response<CentrifugoTokenResponse>

    // ---- Draw ----
    @GET("v1/draw")
    suspend fun getDraw(
        @Query("long1") long1: Double,
        @Query("lat1") lat1: Double,
        @Query("long2") long2: Double,
        @Query("lat2") lat2: Double,
    ): DrawResponse

    // ---- Livekit ----
    @GET("v1/livekit/join")
    suspend fun generateLivekitToken(): Response<LivekitResponse>

    @GET("v1/livekit/participant")
    suspend fun generateListParticipant(): Response<ParticipantsResponse>

    // ---- User / POC info ----
    @GET("v1/mobile/auth/me")
    suspend fun getMe(): Response<UserResponse>

    @GET("v1/mobile/auth/me")
    suspend fun getPocInfo(): Response<PocResponse>
}
