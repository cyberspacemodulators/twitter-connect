package cyberspace.modulator.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.istack.internal.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TwitterConfig {

    @JsonProperty("consumerKey")
    @NotNull
    private String consumerKey;

    @JsonProperty("consumerSecret")
    @NotNull
    private String consumerSecret;

    @JsonProperty("token")
    @NotNull
    private String token;

    @JsonProperty("tokenSecret")
    @NotNull
    private String tokenSecret;

    public TwitterConfig() {

    }

    public String getConsumerKey() {
        return this.consumerKey;
    }

    public String getConsumerSecret() {
        return this.consumerSecret;
    }

    public String getToken() {
        return this.token;
    }

    public String getTokenSecret() {
        return this.tokenSecret;
    }
}
