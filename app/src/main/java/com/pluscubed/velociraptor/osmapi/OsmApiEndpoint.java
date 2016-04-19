package com.pluscubed.velociraptor.osmapi;

import android.support.annotation.NonNull;

import com.pluscubed.velociraptor.utils.Utils;

import java.text.DateFormat;
import java.util.Date;

public class OsmApiEndpoint implements Comparable<OsmApiEndpoint> {
    //Wait 1 minute before trying a slow/errored endpoint again.
    public long timeTakenTimestamp;
    public int timeTaken;
    public String baseUrl;

    public OsmApiEndpoint(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String toString() {
        String time = timeTaken + "ms";
        if (timeTaken == Integer.MAX_VALUE) {
            time = "error";
        } else if (timeTaken == 0) {
            time = "pending";
        }
        return baseUrl + " - " + time +
                ", timestamp " + DateFormat.getTimeInstance().format(new Date(timeTakenTimestamp));
    }

    @Override
    public int compareTo(@NonNull OsmApiEndpoint another) {
        long currentTime = System.currentTimeMillis();

        int compare = Utils.compare(timeTaken, another.timeTaken);

        if (compare == 1 && currentTime > timeTakenTimestamp + 60000) {
            return -1;
        } else if (compare == -1 && currentTime > another.timeTakenTimestamp + 60000) {
            return 1;
        }
        return compare;
    }
}
