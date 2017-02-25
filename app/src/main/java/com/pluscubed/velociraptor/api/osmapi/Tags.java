package com.pluscubed.velociraptor.api.osmapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

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

    @JsonProperty("highway")
    private String highway;
    @JsonProperty("maxspeed")
    private String maxspeed;
    @JsonProperty("name")
    private String name;
    @JsonProperty("ref")
    private String ref;

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

}
