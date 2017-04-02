package com.pluscubed.velociraptor.api.hereapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkInfo {

    @JsonProperty("response")
    private HereResponse response;

    /**
     * @return The response
     */
    @JsonProperty("response")
    public HereResponse getResponse() {
        return response;
    }

    /**
     * @param Response The response
     */
    @JsonProperty("response")
    public void setResponse(HereResponse Response) {
        this.response = Response;
    }

}
