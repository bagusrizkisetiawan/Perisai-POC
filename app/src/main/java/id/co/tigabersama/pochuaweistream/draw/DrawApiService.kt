package id.co.tigabersama.pochuaweistream.draw

import retrofit2.http.GET
import retrofit2.http.Query

interface DrawApiService {
    @GET("v1/draw")
    suspend fun getDraw(
        @Query("long1") long1: Double,
        @Query("lat1")  lat1: Double,
        @Query("long2") long2: Double,
        @Query("lat2")  lat2: Double
    ): DrawResponse
}

