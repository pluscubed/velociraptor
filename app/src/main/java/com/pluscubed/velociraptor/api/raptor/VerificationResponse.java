package com.pluscubed.velociraptor.api.raptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificationResponse {
    @JsonProperty("token")
    private String token;

    @JsonProperty("token")
    public String getToken() {
        return token;
    }
}
