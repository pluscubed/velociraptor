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
        "MapVersion",
        "ModuleVersion",
        "InterfaceVersion",
        "Timestamp"
})
public class MetaInfo {

    @JsonProperty("MapVersion")
    private String MapVersion;
    @JsonProperty("ModuleVersion")
    private String ModuleVersion;
    @JsonProperty("InterfaceVersion")
    private String InterfaceVersion;
    @JsonProperty("Timestamp")
    private String Timestamp;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * @return The MapVersion
     */
    @JsonProperty("MapVersion")
    public String getMapVersion() {
        return MapVersion;
    }

    /**
     * @param MapVersion The MapVersion
     */
    @JsonProperty("MapVersion")
    public void setMapVersion(String MapVersion) {
        this.MapVersion = MapVersion;
    }

    /**
     * @return The ModuleVersion
     */
    @JsonProperty("ModuleVersion")
    public String getModuleVersion() {
        return ModuleVersion;
    }

    /**
     * @param ModuleVersion The ModuleVersion
     */
    @JsonProperty("ModuleVersion")
    public void setModuleVersion(String ModuleVersion) {
        this.ModuleVersion = ModuleVersion;
    }

    /**
     * @return The InterfaceVersion
     */
    @JsonProperty("InterfaceVersion")
    public String getInterfaceVersion() {
        return InterfaceVersion;
    }

    /**
     * @param InterfaceVersion The InterfaceVersion
     */
    @JsonProperty("InterfaceVersion")
    public void setInterfaceVersion(String InterfaceVersion) {
        this.InterfaceVersion = InterfaceVersion;
    }

    /**
     * @return The Timestamp
     */
    @JsonProperty("Timestamp")
    public String getTimestamp() {
        return Timestamp;
    }

    /**
     * @param Timestamp The Timestamp
     */
    @JsonProperty("Timestamp")
    public void setTimestamp(String Timestamp) {
        this.Timestamp = Timestamp;
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
