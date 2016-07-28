package com.pluscubed.velociraptor.api.osmapi;

import android.location.Location;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

@JsonPropertyOrder({
        "lat",
        "lon"
})

public class Coord {

    @JsonProperty("lat")
    public double lat;
    @JsonProperty("lon")
    public double lon;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    public Coord() {
        super();
    }

    public Coord(Double lat, Double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public Coord(Location location) {
        lat = location.getLatitude();
        lon = location.getLongitude();
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Location toLocation() {
        Location loc = new Location("");
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        return loc;
    }

    @Override
    public String toString() {
        return lat + "," + lon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Coord coord = (Coord) o;

        if (Double.compare(coord.lat, lat) != 0) return false;
        return Double.compare(coord.lon, lon) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(lat);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lon);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
