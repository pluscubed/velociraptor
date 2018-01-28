package com.pluscubed.velociraptor.api;

import android.content.Context;

import com.google.auto.value.AutoValue;
import com.pluscubed.velociraptor.R;

import org.jetbrains.annotations.Nullable;

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
                .setDebugInfo("")
                .setError(null)
                .setSpeedLimit(-1)
                .setRoadName("")
                .setCoords(new ArrayList<>())
                .setTimestamp(0);
    }

    static String getLimitProviderString(Context context, int origin) {
        String provider = "";
        switch (origin) {
            case LimitResponse.ORIGIN_HERE:
                provider = context.getString(R.string.here_provider_short);
                break;
            case LimitResponse.ORIGIN_TOMTOM:
                provider = context.getString(R.string.tomtom_provider_short);
                break;
            case LimitResponse.ORIGIN_OSM:
                provider = context.getString(R.string.openstreetmap_short);
                break;
            case -1:
                provider = "?";
                break;
            default:
                provider = String.valueOf(origin);
        }
        return provider;
    }

    // Generated
    public abstract boolean fromCache();

    public abstract String debugInfo();

    public abstract int origin();

    @Nullable
    public abstract Throwable error();

    /**
     * In km/h, -1 if limit does not exist
     */
    public abstract int speedLimit();

    public abstract String roadName();

    public abstract List<Coord> coords();

    public abstract long timestamp();

    public abstract Builder toBuilder();

    public boolean isEmpty() {
        return coords().isEmpty();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setFromCache(boolean value);
        public abstract Builder setOrigin(int value);

        public abstract Builder setError(Throwable value);

        public abstract Builder setDebugInfo(String value);

        public abstract Builder setSpeedLimit(int value);

        public abstract Builder setRoadName(String value);

        public abstract Builder setCoords(List<Coord> value);

        public abstract Builder setTimestamp(long value);

        public abstract LimitResponse build();

        abstract String debugInfo();

        abstract boolean fromCache();

        abstract int origin();

        abstract String roadName();

        abstract List<Coord> coords();

        abstract int speedLimit();

        abstract Throwable error();

        public Builder initDebugInfo(Context context) {
            String origin = getLimitProviderString(context, origin());

            String text =
                    "\nOrigin: " + origin +
                            "\n--From cache: " + fromCache();
            if (error() == null) {
                text += "\n--Road name: " + roadName() +
                        "\n--Coords: " + coords() +
                        "\n--Limit: " + speedLimit();
            } else {
                text += "\n--Error: " + error().toString();
            }

            return setDebugInfo(debugInfo() + text);
        }
    }
}
