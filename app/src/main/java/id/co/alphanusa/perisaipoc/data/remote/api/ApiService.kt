package id.co.alphanusa.perisaipoc.data.remote.api

import id.co.alphanusa.perisaipoc.data.remote.dto.CentrifugoTokenRequestDto
import id.co.alphanusa.perisaipoc.data.remote.dto.CentrifugoTokenResponseDto
import id.co.alphanusa.perisaipoc.data.remote.dto.DrawResponseDto
import id.co.alphanusa.perisaipoc.data.remote.dto.LivekitResponseDto
import id.co.alphanusa.perisaipoc.data.remote.dto.LoginRequestDto
import id.co.alphanusa.perisaipoc.data.remote.dto.LoginResponseDto
import id.co.alphanusa.perisaipoc.data.remote.dto.ParticipantsResponseDto
import id.co.alphanusa.perisaipoc.data.remote.dto.PocResponseDto
import id.co.alphanusa.perisaipoc.data.remote.dto.RefreshTokenRequestDto
import id.co.alphanusa.perisaipoc.data.remote.dto.RefreshTokenResponseDto
import id.co.alphanusa.perisaipoc.data.remote.dto.UserResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Satu-satunya kontrak REST aplikasi. */
interface ApiService {

    // ---- Auth ----
    @POST("v1/mobile/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @POST("v1/mobile/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequestDto,
    ): Response<RefreshTokenResponseDto>

    // ---- Realtime (Centrifugo) ----
    @POST("v1/mobile/auth/gentoken-centrifugo")
    suspend fun generateCentrifugoToken(
        @Body request: CentrifugoTokenRequestDto,
    ): Response<CentrifugoTokenResponseDto>

    // ---- Peta ----
    @GET("v1/draw")
    suspend fun getDraw(
        @Query("long1") long1: Double,
        @Query("lat1") lat1: Double,
        @Query("long2") long2: Double,
        @Query("lat2") lat2: Double,
    ): DrawResponseDto

    // ---- Call (LiveKit) ----
    @GET("v1/livekit/join")
    suspend fun joinLivekitRoom(): Response<LivekitResponseDto>

    @GET("v1/livekit/participant")
    suspend fun getLivekitParticipants(): Response<ParticipantsResponseDto>

    // ---- Profil & perangkat (endpoint sama, bentuk respons berbeda) ----
    @GET("v1/mobile/auth/me")
    suspend fun getUserProfile(): Response<UserResponseDto>

    @GET("v1/mobile/auth/me")
    suspend fun getPocDevice(): Response<PocResponseDto>
}
