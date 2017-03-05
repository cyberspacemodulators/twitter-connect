package cyberspace.modulator.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.istack.internal.NotNull;

import java.util.List;

public class CyberSpaceModulatorConfig {

    @JsonProperty(value = "sendToArduino", defaultValue = "false")
    private Boolean sendToArduino;

    @JsonProperty(value = "setUpGui", defaultValue = "false")
    private Boolean setUpGui;

    @JsonProperty("extractedData")
    @NotNull
    private ExtractedData extractedData;

    @JsonProperty("twitter")
    @NotNull
    private TwitterConfig twitterConfig;

    @JsonProperty("trackTerms")
    private List<String> trackTerms;

    @JsonProperty("followIds")
    private List<Long> followIds;

    /**
     * Empty default constructor for serialization
     */
    public CyberSpaceModulatorConfig() {

    }

    public TwitterConfig getTwitterConfig() {
        return this.twitterConfig;
    }

    public List<String> getTrackTerms() {
        return this.trackTerms;
    }

    public List<Long> getFollowIds() {
        return this.followIds;
    }

    public Boolean getSendToArduino() {
        return this.sendToArduino;
    }

    public ExtractedData getExtractedData() {
        return this.extractedData;
    }

    public Boolean getSetUpGui() {
        return this.setUpGui;
    }
}
