package com.pluscubed.velociraptor.api.hereapi;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Single;

public interface HereService {
    @GET("getlinkinfo.json")
    Single<LinkInfo> getLinkInfo(@Query("waypoint") String waypoint, @Query("app_id") String appId, @Query("app_code") String appCode);
}
