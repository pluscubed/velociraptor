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
        "MetaInfo",
        "Link"
})
public class Response {

    @JsonProperty("MetaInfo")
    private MetaInfo MetaInfo;
    @JsonProperty("Link")
    private List<Link> Link = new ArrayList<>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * @return The MetaInfo
     */
    @JsonProperty("MetaInfo")
    public MetaInfo getMetaInfo() {
        return MetaInfo;
    }

    /**
     * @param MetaInfo The MetaInfo
     */
    @JsonProperty("MetaInfo")
    public void setMetaInfo(MetaInfo MetaInfo) {
        this.MetaInfo = MetaInfo;
    }

    /**
     * @return The Link
     */
    @JsonProperty("Link")
    public List<Link> getLink() {
        return Link;
    }

    /**
     * @param Link The Link
     */
    @JsonProperty("Link")
    public void setLink(List<Link> Link) {
        this.Link = Link;
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
