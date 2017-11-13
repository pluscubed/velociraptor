package com.pluscubed.velociraptor.api.raptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class RaptorResponse {

    // mph or km/h depending on country... gah!
    @JsonProperty("general_speed_limit")
    private Integer generalSpeedLimit;
    @JsonProperty("name")
    private String name;
    @JsonProperty("polyline")
    private String polyline;

    @JsonProperty("general_speed_limit")
    public Integer getGeneralSpeedLimit() {
        return generalSpeedLimit;
    }

    @JsonProperty("general_speed_limit")
    public void setGeneralSpeedLimit(Integer generalSpeedLimit) {
        this.generalSpeedLimit = generalSpeedLimit;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("polyline")
    public String getPolyline() {
        return polyline;
    }

    @JsonProperty("polyline")
    public void setPolyline(String polyline) {
        this.polyline = polyline;
    }
}
