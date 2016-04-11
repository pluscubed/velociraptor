
package com.pluscubed.velociraptor.osm;

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
        "source:maxspeed",
        "tiger:cfcc",
        "tiger:county",
        "tiger:name_base",
        "tiger:name_base_1",
        "tiger:name_direction_prefix",
        "tiger:zip_left",
        "tiger:zip_right"
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
    @JsonProperty("tiger:cfcc")
    private String tigerCfcc;
    @JsonProperty("tiger:county")
    private String tigerCounty;
    @JsonProperty("tiger:name_base")
    private String tigerNameBase;
    @JsonProperty("tiger:name_base_1")
    private String tigerNameBase1;
    @JsonProperty("tiger:name_direction_prefix")
    private String tigerNameDirectionPrefix;
    @JsonProperty("tiger:zip_left")
    private String tigerZipLeft;
    @JsonProperty("tiger:zip_right")
    private String tigerZipRight;
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

    /**
     * @return The tigerCfcc
     */
    @JsonProperty("tiger:cfcc")
    public String getTigerCfcc() {
        return tigerCfcc;
    }

    /**
     * @param tigerCfcc The tiger:cfcc
     */
    @JsonProperty("tiger:cfcc")
    public void setTigerCfcc(String tigerCfcc) {
        this.tigerCfcc = tigerCfcc;
    }

    /**
     * @return The tigerCounty
     */
    @JsonProperty("tiger:county")
    public String getTigerCounty() {
        return tigerCounty;
    }

    /**
     * @param tigerCounty The tiger:county
     */
    @JsonProperty("tiger:county")
    public void setTigerCounty(String tigerCounty) {
        this.tigerCounty = tigerCounty;
    }

    /**
     * @return The tigerNameBase
     */
    @JsonProperty("tiger:name_base")
    public String getTigerNameBase() {
        return tigerNameBase;
    }

    /**
     * @param tigerNameBase The tiger:name_base
     */
    @JsonProperty("tiger:name_base")
    public void setTigerNameBase(String tigerNameBase) {
        this.tigerNameBase = tigerNameBase;
    }

    /**
     * @return The tigerNameBase1
     */
    @JsonProperty("tiger:name_base_1")
    public String getTigerNameBase1() {
        return tigerNameBase1;
    }

    /**
     * @param tigerNameBase1 The tiger:name_base_1
     */
    @JsonProperty("tiger:name_base_1")
    public void setTigerNameBase1(String tigerNameBase1) {
        this.tigerNameBase1 = tigerNameBase1;
    }

    /**
     * @return The tigerNameDirectionPrefix
     */
    @JsonProperty("tiger:name_direction_prefix")
    public String getTigerNameDirectionPrefix() {
        return tigerNameDirectionPrefix;
    }

    /**
     * @param tigerNameDirectionPrefix The tiger:name_direction_prefix
     */
    @JsonProperty("tiger:name_direction_prefix")
    public void setTigerNameDirectionPrefix(String tigerNameDirectionPrefix) {
        this.tigerNameDirectionPrefix = tigerNameDirectionPrefix;
    }

    /**
     * @return The tigerZipLeft
     */
    @JsonProperty("tiger:zip_left")
    public String getTigerZipLeft() {
        return tigerZipLeft;
    }

    /**
     * @param tigerZipLeft The tiger:zip_left
     */
    @JsonProperty("tiger:zip_left")
    public void setTigerZipLeft(String tigerZipLeft) {
        this.tigerZipLeft = tigerZipLeft;
    }

    /**
     * @return The tigerZipRight
     */
    @JsonProperty("tiger:zip_right")
    public String getTigerZipRight() {
        return tigerZipRight;
    }

    /**
     * @param tigerZipRight The tiger:zip_right
     */
    @JsonProperty("tiger:zip_right")
    public void setTigerZipRight(String tigerZipRight) {
        this.tigerZipRight = tigerZipRight;
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
