package com.pluscubed.velociraptor.api.osm

import com.pluscubed.velociraptor.api.osm.data.OsmResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface OsmService {
    @POST("interpreter")
    fun getOsm(@Body data: String): Call<OsmResponse?>
}
