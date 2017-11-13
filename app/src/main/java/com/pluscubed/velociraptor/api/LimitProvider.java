package com.pluscubed.velociraptor.api;

import android.location.Location;

import rx.Observable;

public interface LimitProvider {

    /**
     * Returns response (regardless of whether there is speed limit) and caches each way received,
     * or empty if there is no road information
     */
    Observable<LimitResponse> getSpeedLimit(Location location, LimitResponse lastResponse);
}
