
package com.pluscubed.velociraptor.hereapi;

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
        "Label",
        "Country",
        "State",
        "County",
        "City",
        "District",
        "Street"
})
public class Address {

    @JsonProperty("Label")
    private String Label;
    @JsonProperty("Country")
    private String Country;
    @JsonProperty("State")
    private String State;
    @JsonProperty("County")
    private String County;
    @JsonProperty("City")
    private String City;
    @JsonProperty("District")
    private String District;
    @JsonProperty("Street")
    private String Street;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * @return The Label
     */
    @JsonProperty("Label")
    public String getLabel() {
        return Label;
    }

    /**
     * @param Label The Label
     */
    @JsonProperty("Label")
    public void setLabel(String Label) {
        this.Label = Label;
    }

    /**
     * @return The Country
     */
    @JsonProperty("Country")
    public String getCountry() {
        return Country;
    }

    /**
     * @param Country The Country
     */
    @JsonProperty("Country")
    public void setCountry(String Country) {
        this.Country = Country;
    }

    /**
     * @return The State
     */
    @JsonProperty("State")
    public String getState() {
        return State;
    }

    /**
     * @param State The State
     */
    @JsonProperty("State")
    public void setState(String State) {
        this.State = State;
    }

    /**
     * @return The County
     */
    @JsonProperty("County")
    public String getCounty() {
        return County;
    }

    /**
     * @param County The County
     */
    @JsonProperty("County")
    public void setCounty(String County) {
        this.County = County;
    }

    /**
     * @return The City
     */
    @JsonProperty("City")
    public String getCity() {
        return City;
    }

    /**
     * @param City The City
     */
    @JsonProperty("City")
    public void setCity(String City) {
        this.City = City;
    }

    /**
     * @return The District
     */
    @JsonProperty("District")
    public String getDistrict() {
        return District;
    }

    /**
     * @param District The District
     */
    @JsonProperty("District")
    public void setDistrict(String District) {
        this.District = District;
    }

    /**
     * @return The Street
     */
    @JsonProperty("Street")
    public String getStreet() {
        return Street;
    }

    /**
     * @param Street The Street
     */
    @JsonProperty("Street")
    public void setStreet(String Street) {
        this.Street = Street;
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
