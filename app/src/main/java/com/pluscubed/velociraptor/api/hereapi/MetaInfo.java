package com.pluscubed.velociraptor.api.hereapi;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class MetaInfo {

    @JsonProperty("mapVersion")
    private String MapVersion;
    @JsonProperty("moduleVersion")
    private String ModuleVersion;
    @JsonProperty("interfaceVersion")
    private String InterfaceVersion;
    @JsonProperty("timestamp")
    private String Timestamp;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * @return The MapVersion
     */
    @JsonProperty("mapVersion")
    public String getMapVersion() {
        return MapVersion;
    }

    /**
     * @param MapVersion The MapVersion
     */
    @JsonProperty("mapVersion")
    public void setMapVersion(String MapVersion) {
        this.MapVersion = MapVersion;
    }

    /**
     * @return The ModuleVersion
     */
    @JsonProperty("moduleVersion")
    public String getModuleVersion() {
        return ModuleVersion;
    }

    /**
     * @param ModuleVersion The ModuleVersion
     */
    @JsonProperty("moduleVersion")
    public void setModuleVersion(String ModuleVersion) {
        this.ModuleVersion = ModuleVersion;
    }

    /**
     * @return The InterfaceVersion
     */
    @JsonProperty("interfaceVersion")
    public String getInterfaceVersion() {
        return InterfaceVersion;
    }

    /**
     * @param InterfaceVersion The InterfaceVersion
     */
    @JsonProperty("interfaceVersion")
    public void setInterfaceVersion(String InterfaceVersion) {
        this.InterfaceVersion = InterfaceVersion;
    }

    /**
     * @return The Timestamp
     */
    @JsonProperty("timestamp")
    public String getTimestamp() {
        return Timestamp;
    }

    /**
     * @param Timestamp The Timestamp
     */
    @JsonProperty("timestamp")
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
