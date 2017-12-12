package com.pluscubed.velociraptor.api;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.List;

@AutoValue
public abstract class LimitResponse {

    public static final int ORIGIN_HERE = 2;
    public static final int ORIGIN_TOMTOM = 1;
    public static final int ORIGIN_OSM = 0;

    public static Builder builder() {
        return new AutoValue_LimitResponse.Builder()
                .setFromCache(false)
                .setOrigin(-1)
                .setSpeedLimit(-1)
                .setRoadName("")
                .setCoords(new ArrayList<>())
                .setTimestamp(0);
    }

    public abstract boolean fromCache();

    public abstract int origin();

    /**
     * In km/h, -1 if limit does not exist
     */
    public abstract int speedLimit();

    public abstract String roadName();

    public abstract List<Coord> coords();

    public abstract long timestamp();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setFromCache(boolean value);

        public abstract Builder setOrigin(int value);

        public abstract Builder setSpeedLimit(int value);

        public abstract Builder setRoadName(String value);

        public abstract Builder setCoords(List<Coord> value);

        public abstract Builder setTimestamp(long value);

        public abstract LimitResponse build();
    }
}
