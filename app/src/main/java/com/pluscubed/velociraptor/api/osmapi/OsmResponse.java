package com.pluscubed.velociraptor.api.osmapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class OsmResponse {

    @JsonProperty("osm3s")
    private Osm3s osm3s;
    @JsonProperty("elements")
    private List<Element> elements = new ArrayList<>();

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

}
