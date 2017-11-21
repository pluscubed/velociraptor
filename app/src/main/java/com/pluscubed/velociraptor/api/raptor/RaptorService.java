package com.pluscubed.velociraptor.api.raptor;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Single;

public interface RaptorService {
    @Headers("Content-Type: application/json")
    @POST("verify/{id}")
    Single<VerificationResponse> verify(@Path("id") String id, @Body String data);

    @GET("tomtom")
    Single<RaptorResponse> getTomtom(@Header("Authorization") String authorization, @Query("id") String id,
                                     @Query("lat") String lat, @Query("lng") String lng, @Query("vehicle_heading") int heading);

    @GET("here")
    Single<RaptorResponse> getHere(@Header("Authorization") String authorization, @Query("id") String id,
                                   @Query("lat") String lat, @Query("lng") String lng, @Query("vehicle_heading") int heading);
}
