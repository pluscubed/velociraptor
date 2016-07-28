package com.pluscubed.velociraptor.api.osmapi;

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
        "hgv",
        "hgv:national_network",
        "highway",
        "lanes",
        "maxspeed",
        "name",
        "old_ref",
        "oneway",
        "ref",
        "source:hgv:national_network",
        "source:maxspeed"
})
public class Tags {

    @JsonProperty("hgv")
    private String hgv;
    @JsonProperty("hgv:national_network")
    private String hgvNationalNetwork;
    @JsonProperty("highway")
    private String highway;
    @JsonProperty("lanes")
    private String lanes;
    @JsonProperty("maxspeed")
    private String maxspeed;
    @JsonProperty("name")
    private String name;
    @JsonProperty("old_ref")
    private String oldRef;
    @JsonProperty("oneway")
    private String oneway;
    @JsonProperty("ref")
    private String ref;
    @JsonProperty("source:hgv:national_network")
    private String sourceHgvNationalNetwork;
    @JsonProperty("source:maxspeed")
    private String sourceMaxspeed;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * @return The hgv
     */
    @JsonProperty("hgv")
    public String getHgv() {
        return hgv;
    }

    /**
     * @param hgv The hgv
     */
    @JsonProperty("hgv")
    public void setHgv(String hgv) {
        this.hgv = hgv;
    }

    /**
     * @return The hgvNationalNetwork
     */
    @JsonProperty("hgv:national_network")
    public String getHgvNationalNetwork() {
        return hgvNationalNetwork;
    }

    /**
     * @param hgvNationalNetwork The hgv:national_network
     */
    @JsonProperty("hgv:national_network")
    public void setHgvNationalNetwork(String hgvNationalNetwork) {
        this.hgvNationalNetwork = hgvNationalNetwork;
    }

    /**
     * @return The highway
     */
    @JsonProperty("highway")
    public String getHighway() {
        return highway;
    }

    /**
     * @param highway The highway
     */
    @JsonProperty("highway")
    public void setHighway(String highway) {
        this.highway = highway;
    }

    /**
     * @return The lanes
     */
    @JsonProperty("lanes")
    public String getLanes() {
        return lanes;
    }

    /**
     * @param lanes The lanes
     */
    @JsonProperty("lanes")
    public void setLanes(String lanes) {
        this.lanes = lanes;
    }

    /**
     * @return The maxspeed
     */
    @JsonProperty("maxspeed")
    public String getMaxspeed() {
        return maxspeed;
    }

    /**
     * @param maxspeed The maxspeed
     */
    @JsonProperty("maxspeed")
    public void setMaxspeed(String maxspeed) {
        this.maxspeed = maxspeed;
    }

    /**
     * @return The name
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * @param name The name
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return The oldRef
     */
    @JsonProperty("old_ref")
    public String getOldRef() {
        return oldRef;
    }

    /**
     * @param oldRef The old_ref
     */
    @JsonProperty("old_ref")
    public void setOldRef(String oldRef) {
        this.oldRef = oldRef;
    }

    /**
     * @return The oneway
     */
    @JsonProperty("oneway")
    public String getOneway() {
        return oneway;
    }

    /**
     * @param oneway The oneway
     */
    @JsonProperty("oneway")
    public void setOneway(String oneway) {
        this.oneway = oneway;
    }

    /**
     * @return The ref
     */
    @JsonProperty("ref")
    public String getRef() {
        return ref;
    }

    /**
     * @param ref The ref
     */
    @JsonProperty("ref")
    public void setRef(String ref) {
        this.ref = ref;
    }

    /**
     * @return The sourceHgvNationalNetwork
     */
    @JsonProperty("source:hgv:national_network")
    public String getSourceHgvNationalNetwork() {
        return sourceHgvNationalNetwork;
    }

    /**
     * @param sourceHgvNationalNetwork The source:hgv:national_network
     */
    @JsonProperty("source:hgv:national_network")
    public void setSourceHgvNationalNetwork(String sourceHgvNationalNetwork) {
        this.sourceHgvNationalNetwork = sourceHgvNationalNetwork;
    }

    /**
     * @return The sourceMaxspeed
     */
    @JsonProperty("source:maxspeed")
    public String getSourceMaxspeed() {
        return sourceMaxspeed;
    }

    /**
     * @param sourceMaxspeed The source:maxspeed
     */
    @JsonProperty("source:maxspeed")
    public void setSourceMaxspeed(String sourceMaxspeed) {
        this.sourceMaxspeed = sourceMaxspeed;
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
