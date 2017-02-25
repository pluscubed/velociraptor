package com.pluscubed.velociraptor.api.osmapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class Osm3s {

    @JsonProperty("timestamp_osm_base")
    private String timestampOsmBase;

    /**
     * @return The timestampOsmBase
     */
    @JsonProperty("timestamp_osm_base")
    public String getTimestampOsmBase() {
        return timestampOsmBase;
    }

    /**
     * @param timestampOsmBase The timestamp_osm_base
     */
    @JsonProperty("timestamp_osm_base")
    public void setTimestampOsmBase(String timestampOsmBase) {
        this.timestampOsmBase = timestampOsmBase;
    }

}
