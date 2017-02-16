package com.pluscubed.velociraptor.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pluscubed.velociraptor.api.osmapi.Coord;

import java.util.List;

public class ApiResponse {
    @JsonIgnore
    public boolean fromCache;

    public boolean useHere;
    //-1 if DNE
    public int speedLimit = -1;
    public String roadName;

    public List<Coord> coords;

    /**
     * Time when response is fetched
     */
    public long timestamp;
}
