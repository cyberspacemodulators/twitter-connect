package cyberspace.modulator.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CyberSpaceModulatorConfig {

    @JsonProperty("extractedData")
    private ExtractedData extractedData;

    @JsonProperty("twitter")
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
        return twitterConfig;
    }

    public List<String> getTrackTerms() {
        return trackTerms;
    }

    public List<Long> getFollowIds() {
        return followIds;
    }
}
