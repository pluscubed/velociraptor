package com.pluscubed.velociraptor.api;

import android.support.annotation.NonNull;

import com.pluscubed.velociraptor.utils.Utils;

public class OsmApiEndpoint implements Comparable<OsmApiEndpoint> {
    public final String baseUrl;
    //Wait 1 minute before trying a slow/errored endpoint again.
    public long timeTakenTimestamp;
    public int timeTaken;
    //null if public
    public String name;

    public boolean enabled;

    public OsmApiEndpoint(String baseUrl, boolean enabled) {
        this.baseUrl = baseUrl;
        this.enabled = enabled;
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

        return title + " - " + enabled + ", " + time + ", " + timeTakenTimestamp;
    }

    @Override
    public int compareTo(@NonNull OsmApiEndpoint another) {
        long currentTime = System.currentTimeMillis();

        int compare = Utils.compare(timeTaken, another.timeTaken);

        if (compare == 1 && (name != null && another.name == null && timeTaken != Integer.MAX_VALUE ||
                currentTime > timeTakenTimestamp + 60000)) {
            return -1;
        } else if (compare == -1 && (another.name != null && name == null && another.timeTaken != Integer.MAX_VALUE ||
                currentTime > another.timeTakenTimestamp + 60000)) {
            return 1;
        }
        return compare;
    }
}
