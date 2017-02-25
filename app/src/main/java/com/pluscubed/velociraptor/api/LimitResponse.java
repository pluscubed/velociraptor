package com.pluscubed.velociraptor.api;

import com.google.auto.value.AutoValue;
import com.pluscubed.velociraptor.api.osmapi.Coord;

import java.util.ArrayList;
import java.util.List;

@AutoValue
public abstract class LimitResponse {
    public static Builder builder() {
        return new AutoValue_LimitResponse.Builder()
                .setFromCache(false)
                .setFromHere(false)
                .setSpeedLimit(-1)
                .setRoadName("")
                .setCoords(new ArrayList<>())
                .setTimestamp(0);
    }

    public abstract boolean fromCache();

    public abstract boolean fromHere();

    /**
     * -1 if limit does not exist
     */
    public abstract int speedLimit();

    public abstract String roadName();

    public abstract List<Coord> coords();

    public abstract long timestamp();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setFromCache(boolean value);

        public abstract Builder setFromHere(boolean value);

        public abstract Builder setSpeedLimit(int value);

        public abstract Builder setRoadName(String value);

        public abstract Builder setCoords(List<Coord> value);

        public abstract Builder setTimestamp(long value);

        public abstract LimitResponse build();
    }
}
