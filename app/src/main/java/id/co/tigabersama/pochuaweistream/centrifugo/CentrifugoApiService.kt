package id.co.tigabersama.surveillance.centrifugo

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CentrifugoApiService {
    @POST("v1/mobile/auth/gentoken-centrifugo")
    suspend fun generateCentrifugoToken(
        @Body request: CentrifugoTokenRequest
    ): Response<CentrifugoTokenResponse>
}
