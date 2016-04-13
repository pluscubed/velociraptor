package com.pluscubed.velociraptor.osmapi;

import android.support.annotation.NonNull;

import com.pluscubed.velociraptor.utils.Utils;

public class OsmApiEndpoint implements Comparable<OsmApiEndpoint> {
    public int timeTaken;
    public String baseUrl;

    public OsmApiEndpoint(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public int compareTo(@NonNull OsmApiEndpoint another) {
        return Utils.compare(timeTaken, another.timeTaken);
    }
}
