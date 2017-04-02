package com.pluscubed.velociraptor.api.hereapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HereResponse {

    @JsonProperty("link")
    private List<Link> link = new ArrayList<>();

    /**
     * @return The link
     */
    @JsonProperty("link")
    public List<Link> getLink() {
        return link;
    }

    /**
     * @param Link The link
     */
    @JsonProperty("link")
    public void setLink(List<Link> Link) {
        this.link = Link;
    }

}
