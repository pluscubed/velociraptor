package com.pluscubed.velociraptor.hereapi;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class LinkInfo {

    @JsonProperty("response")
    private Response Response;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * @return The Response
     */
    @JsonProperty("response")
    public Response getResponse() {
        return Response;
    }

    /**
     * @param Response The Response
     */
    @JsonProperty("response")
    public void setResponse(Response Response) {
        this.Response = Response;
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
