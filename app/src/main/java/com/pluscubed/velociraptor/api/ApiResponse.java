package com.pluscubed.velociraptor.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pluscubed.velociraptor.api.osmapi.Coord;

import java.util.Arrays;
import java.util.List;

public class ApiResponse {
    @JsonIgnore
    public boolean fromCache;

    public boolean useHere;
    //-1 if DNE
    public int speedLimit = -1;
    public String[] roadNames;

    public List<Coord> coords;
    public long timestamp;

    @Override
    public String toString() {
        return "ApiResponse{" +
                "useHere=" + useHere +
                ", speedLimit=" + speedLimit +
                ", roadNames=" + Arrays.toString(roadNames) +
                ", coords=" + coords +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApiResponse that = (ApiResponse) o;

        if (useHere != that.useHere) return false;
        if (speedLimit != that.speedLimit) return false;
        if (timestamp != that.timestamp) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(roadNames, that.roadNames)) return false;
        return coords != null ? coords.equals(that.coords) : that.coords == null;

    }

    @Override
    public int hashCode() {
        int result = (useHere ? 1 : 0);
        result = 31 * result + speedLimit;
        result = 31 * result + Arrays.hashCode(roadNames);
        result = 31 * result + (coords != null ? coords.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
