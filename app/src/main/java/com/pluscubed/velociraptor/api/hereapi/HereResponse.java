package com.pluscubed.velociraptor.api.hereapi;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class HereResponse {

    @JsonProperty("metaInfo")
    private MetaInfo MetaInfo;
    @JsonProperty("link")
    private List<Link> Link = new ArrayList<>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * @return The MetaInfo
     */
    @JsonProperty("metaInfo")
    public MetaInfo getMetaInfo() {
        return MetaInfo;
    }

    /**
     * @param MetaInfo The MetaInfo
     */
    @JsonProperty("metaInfo")
    public void setMetaInfo(MetaInfo MetaInfo) {
        this.MetaInfo = MetaInfo;
    }

    /**
     * @return The Link
     */
    @JsonProperty("link")
    public List<Link> getLink() {
        return Link;
    }

    /**
     * @param Link The Link
     */
    @JsonProperty("link")
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
