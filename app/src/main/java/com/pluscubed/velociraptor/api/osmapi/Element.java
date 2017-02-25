package com.pluscubed.velociraptor.api.osmapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class Element {

    @JsonProperty("id")
    private Integer id;
    @JsonProperty("geometry")
    private List<Coord> geometry = new ArrayList<>();
    @JsonProperty("nodes")
    private List<Long> nodes = new ArrayList<>();
    @JsonProperty("tags")
    private Tags tags;

    /**
     * @return The id
     */
    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    /**
     * @param id The id
     */
    @JsonProperty("id")
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return The tags
     */
    @JsonProperty("tags")
    public Tags getTags() {
        return tags;
    }

    /**
     * @param tags The tags
     */
    @JsonProperty("tags")
    public void setTags(Tags tags) {
        this.tags = tags;
    }

    /**
     * @return The nodes
     */
    @JsonProperty("nodes")
    public List<Long> getNodes() {
        return nodes;
    }

    /**
     * @param nodes The nodes
     */
    @JsonProperty("nodes")
    public void setNodes(List<Long> nodes) {
        this.nodes = nodes;
    }

    @JsonProperty("geometry")
    public List<Coord> getGeometry() {
        return geometry;
    }

    @JsonProperty("geometry")
    public void setGeometry(List<Coord> geometry) {
        this.geometry = geometry;
    }
}
