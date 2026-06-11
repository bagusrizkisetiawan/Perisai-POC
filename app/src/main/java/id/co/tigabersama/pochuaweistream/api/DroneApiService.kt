package id.co.tigabersama.surveillance.api

import retrofit2.Response
import retrofit2.http.GET

interface DroneApiService {
    @GET("v1/mobile/auth/me")
    suspend fun getDroneInfo(): Response<DroneResponse>
}

data class DroneData(
    val ID: String,
    val name: String,
    val serialNumber: String,
    val key: String,
    val agencyId: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double
)

data class DroneResponse(
    val message: String,
    val data: DroneData
)