package com.pluscubed.velociraptor.api.osmapi;

import android.support.annotation.NonNull;

import com.pluscubed.velociraptor.utils.Utils;

public class OsmApiEndpoint implements Comparable<OsmApiEndpoint> {
    //Wait 1 minute before trying a slow/errored endpoint again.
    public long timeTakenTimestamp;
    public int timeTaken;
    public String baseUrl;
    public String name;

    public OsmApiEndpoint(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String toString() {
        String time;
        if (timeTaken == Integer.MAX_VALUE) {
            time = "error";
        } else if (timeTaken == 0) {
            time = "pending";
        } else {
            time = timeTaken + "ms";
        }

        String title;
        if (name != null) {
            title = name;
        } else {
            title = this.baseUrl;
        }

        return title + " - " + time +
                ", timestamp " + timeTakenTimestamp;
    }

    @Override
    public int compareTo(@NonNull OsmApiEndpoint another) {
        long currentTime = System.currentTimeMillis();

        int compare = Utils.compare(timeTaken, another.timeTaken);

        if (compare == 1 && (name != null && timeTaken != Integer.MAX_VALUE ||
                currentTime > timeTakenTimestamp + 60000)) {
            return -1;
        } else if (compare == -1 && (another.name != null && another.timeTaken != Integer.MAX_VALUE ||
                currentTime > another.timeTakenTimestamp + 60000)) {
            return 1;
        }
        return compare;
    }
}
