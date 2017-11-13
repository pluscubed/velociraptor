package com.pluscubed.velociraptor.api.osm;

public class OsmApiEndpoint {
    public final String baseUrl;
    //Wait 1 minute before trying a slow/errored endpoint again.
    public long timeTakenTimestamp;
    public int timeTaken;
    //null if public
    public String name;

    public OsmApiEndpoint(String baseUrl, String name) {
        this.baseUrl = baseUrl;
        this.name = name;
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

        return title + " - " + time + ", " + timeTakenTimestamp;
    }
}
