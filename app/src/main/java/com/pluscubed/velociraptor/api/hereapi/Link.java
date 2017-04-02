package com.pluscubed.velociraptor.api.hereapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Link {

    @JsonProperty("linkId")
    private String linkId;
    @JsonProperty("shape")
    private List<String> shape = new ArrayList<>();
    @JsonProperty("speedLimit")
    private Double speedLimit;
    @JsonProperty("roadName")
    private String roadName;


    /**
     * @return The linkId
     */
    @JsonProperty("linkId")
    public String getLinkId() {
        return linkId;
    }

    /**
     * @param LinkId The linkId
     */
    @JsonProperty("linkId")
    public void setLinkId(String LinkId) {
        this.linkId = LinkId;
    }

    /**
     * @return The shape
     */
    @JsonProperty("shape")
    public List<String> getShape() {
        return shape;
    }

    /**
     * @param Shape The shape
     */
    @JsonProperty("shape")
    public void setShape(List<String> Shape) {
        this.shape = Shape;
    }

    /**
     * @return The speedLimit
     */
    @JsonProperty("speedLimit")
    public Double getSpeedLimit() {
        return speedLimit;
    }

    /**
     * @param SpeedLimit The speedLimit
     */
    @JsonProperty("speedLimit")
    public void setSpeedLimit(Double SpeedLimit) {
        this.speedLimit = SpeedLimit;
    }

    @JsonProperty("roadName")
    public String getRoadName() {
        return roadName;
    }

    @JsonProperty("roadName")
    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

}
