package cyberspace.modulator.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class TwitterConfig {

    @JsonProperty("consumerKey")
    private String consumerKey;

    @JsonProperty("consumerSecret")
    private String consumerSecret;

    @JsonProperty("token")
    private String token;

    @JsonProperty("tokenSecret")
    private String tokenSecret;

    public TwitterConfig() {

    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public String getToken() {
        return token;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }
}
