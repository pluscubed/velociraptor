package com.pluscubed.velociraptor.api;

import android.location.Location;

import rx.Observable;

public interface LimitProvider {

    /**
     * Returns all responses and caches each way received (regardless of whether there is speed limit)
     */
    Observable<LimitResponse> getSpeedLimit(Location location, LimitResponse lastResponse);
}
