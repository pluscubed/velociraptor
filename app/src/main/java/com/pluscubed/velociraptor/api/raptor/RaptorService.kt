package com.pluscubed.velociraptor.api.raptor

import kotlinx.coroutines.Deferred
import retrofit2.http.*

interface RaptorService {
    @Headers("Content-Type: application/json")
    @POST("verify/{id}")
    fun verify(
        @Path("id") id: String,
        @Body data: String
    )
            : Deferred<VerificationResponse>

    @GET("tomtom")
    fun getTomtom(
        @Header("Authorization") authorization: String,
        @Query("id") id: String,
        @Query("lat") lat: String,
        @Query("lng") lng: String,
        @Query("vehicle_heading") heading: Int,
        @Query("units") units: String
    )
            : Deferred<RaptorResponse>

    @GET("here")
    fun getHere(
        @Header("Authorization") authorization: String,
        @Query("id") id: String,
        @Query("lat") lat: String,
        @Query("lng") lng: String,
        @Query("vehicle_heading") heading: Int,
        @Query("units") units: String
    )
            : Deferred<RaptorResponse>
}
