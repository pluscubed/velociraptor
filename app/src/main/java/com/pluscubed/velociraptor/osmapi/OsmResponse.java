
package com.pluscubed.velociraptor.osmapi;

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
        "version",
        "generator",
        "osm3s",
        "elements"
})
public class OsmResponse {

    @JsonProperty("version")
    private Double version;
    @JsonProperty("generator")
    private String generator;
    @JsonProperty("osm3s")
    private Osm3s osm3s;
    @JsonProperty("elements")
    private List<Element> elements = new ArrayList<>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * @return The version
     */
    @JsonProperty("version")
    public Double getVersion() {
        return version;
    }

    /**
     * @param version The version
     */
    @JsonProperty("version")
    public void setVersion(Double version) {
        this.version = version;
    }

    /**
     * @return The generator
     */
    @JsonProperty("generator")
    public String getGenerator() {
        return generator;
    }

    /**
     * @param generator The generator
     */
    @JsonProperty("generator")
    public void setGenerator(String generator) {
        this.generator = generator;
    }

    /**
     * @return The osm3s
     */
    @JsonProperty("osm3s")
    public Osm3s getOsm3s() {
        return osm3s;
    }

    /**
     * @param osm3s The osm3s
     */
    @JsonProperty("osm3s")
    public void setOsm3s(Osm3s osm3s) {
        this.osm3s = osm3s;
    }

    /**
     * @return The elements
     */
    @JsonProperty("elements")
    public List<Element> getElements() {
        return elements;
    }

    /**
     * @param elements The elements
     */
    @JsonProperty("elements")
    public void setElements(List<Element> elements) {
        this.elements = elements;
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
