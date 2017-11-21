package com.pluscubed.velociraptor.api.osm;

import com.pluscubed.velociraptor.api.osm.data.OsmResponse;

import retrofit2.http.Body;
import retrofit2.http.POST;
import rx.Single;

public interface OsmService {
    @POST("interpreter")
    Single<OsmResponse> getOsm(@Body String data);
}
