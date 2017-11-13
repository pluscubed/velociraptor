package com.pluscubed.velociraptor.api.osm;

import com.pluscubed.velociraptor.api.osm.data.OsmResponse;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Single;

public interface OsmService {
    @GET("interpreter")
    Single<OsmResponse> getOsm(@Query("data") String data);
}
