package com.pluscubed.velociraptor.hereapi;

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
        "TrafficSpeed",
        "TrafficTime",
        "BaseSpeed",
        "BaseTime"
})
public class DynamicSpeedInfo {

    @JsonProperty("TrafficSpeed")
    private Double TrafficSpeed;
    @JsonProperty("TrafficTime")
    private Integer TrafficTime;
    @JsonProperty("BaseSpeed")
    private Double BaseSpeed;
    @JsonProperty("BaseTime")
    private Double BaseTime;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * @return The TrafficSpeed
     */
    @JsonProperty("TrafficSpeed")
    public Double getTrafficSpeed() {
        return TrafficSpeed;
    }

    /**
     * @param TrafficSpeed The TrafficSpeed
     */
    @JsonProperty("TrafficSpeed")
    public void setTrafficSpeed(Double TrafficSpeed) {
        this.TrafficSpeed = TrafficSpeed;
    }

    /**
     * @return The TrafficTime
     */
    @JsonProperty("TrafficTime")
    public Integer getTrafficTime() {
        return TrafficTime;
    }

    /**
     * @param TrafficTime The TrafficTime
     */
    @JsonProperty("TrafficTime")
    public void setTrafficTime(Integer TrafficTime) {
        this.TrafficTime = TrafficTime;
    }

    /**
     * @return The BaseSpeed
     */
    @JsonProperty("BaseSpeed")
    public Double getBaseSpeed() {
        return BaseSpeed;
    }

    /**
     * @param BaseSpeed The BaseSpeed
     */
    @JsonProperty("BaseSpeed")
    public void setBaseSpeed(Double BaseSpeed) {
        this.BaseSpeed = BaseSpeed;
    }

    /**
     * @return The BaseTime
     */
    @JsonProperty("BaseTime")
    public Double getBaseTime() {
        return BaseTime;
    }

    /**
     * @param BaseTime The BaseTime
     */
    @JsonProperty("BaseTime")
    public void setBaseTime(Double BaseTime) {
        this.BaseTime = BaseTime;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
