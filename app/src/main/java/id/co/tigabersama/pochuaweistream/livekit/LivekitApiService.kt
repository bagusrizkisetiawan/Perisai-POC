package id.co.tigabersama.pochuaweistream.livekit

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

interface LivekitApiService {
    @GET("v1/livekit/join")
    suspend fun generateLivekitToken(
    ): Response<LivekitResponse>

    @GET("v1/livekit/participant")
    suspend fun generateListParticipant(
    ): Response<ParticipantsResponse>
}