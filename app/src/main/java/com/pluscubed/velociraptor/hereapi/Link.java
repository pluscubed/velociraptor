package com.pluscubed.velociraptor.hereapi;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

@JsonPropertyOrder({
        "_type",
        "LinkId",
        "Shape",
        "SpeedLimit",
        "DynamicSpeedInfo",
        "Address"
})
public class Link {

    @JsonProperty("_type")
    private String Type;
    @JsonProperty("LinkId")
    private String LinkId;
    @JsonProperty("Shape")
    private List<String> Shape = new ArrayList<String>();
    @JsonProperty("SpeedLimit")
    private Double SpeedLimit;
    @JsonProperty("DynamicSpeedInfo")
    private DynamicSpeedInfo DynamicSpeedInfo;
    @JsonProperty("Address")
    private Address Address;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * @return The Type
     */
    @JsonProperty("_type")
    public String getType() {
        return Type;
    }

    /**
     * @param Type The _type
     */
    @JsonProperty("_type")
    public void setType(String Type) {
        this.Type = Type;
    }

    /**
     * @return The LinkId
     */
    @JsonProperty("LinkId")
    public String getLinkId() {
        return LinkId;
    }

    /**
     * @param LinkId The LinkId
     */
    @JsonProperty("LinkId")
    public void setLinkId(String LinkId) {
        this.LinkId = LinkId;
    }

    /**
     * @return The Shape
     */
    @JsonProperty("Shape")
    public List<String> getShape() {
        return Shape;
    }

    /**
     * @param Shape The Shape
     */
    @JsonProperty("Shape")
    public void setShape(List<String> Shape) {
        this.Shape = Shape;
    }

    /**
     * @return The SpeedLimit
     */
    @JsonProperty("SpeedLimit")
    public Double getSpeedLimit() {
        return SpeedLimit;
    }

    /**
     * @param SpeedLimit The SpeedLimit
     */
    @JsonProperty("SpeedLimit")
    public void setSpeedLimit(Double SpeedLimit) {
        this.SpeedLimit = SpeedLimit;
    }

    /**
     * @return The DynamicSpeedInfo
     */
    @JsonProperty("DynamicSpeedInfo")
    public DynamicSpeedInfo getDynamicSpeedInfo() {
        return DynamicSpeedInfo;
    }

    /**
     * @param DynamicSpeedInfo The DynamicSpeedInfo
     */
    @JsonProperty("DynamicSpeedInfo")
    public void setDynamicSpeedInfo(DynamicSpeedInfo DynamicSpeedInfo) {
        this.DynamicSpeedInfo = DynamicSpeedInfo;
    }

    /**
     * @return The Address
     */
    @JsonProperty("Address")
    public Address getAddress() {
        return Address;
    }

    /**
     * @param Address The Address
     */
    @JsonProperty("Address")
    public void setAddress(Address Address) {
        this.Address = Address;
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
