package cyberspace.modulator;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.junit.Before;
import org.junit.Test;
import twitter4j.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestTwitterConnect {

    enum ExtractedData {
        GEOLOCATION, TWEET_STATISTICS;
    }

    /**
     * Change this depending on your project
     */
    private static final ExtractedData EXTRACTED_DATA = ExtractedData.TWEET_STATISTICS;

    private static final String CONSUMER_KEY = "pTlsXUDKFFQH4ewrQiZWO62g4";

    private static final String CONSUMER_SECRET = "yVESn0bstElSiY8pluhRBHoCeiIEiPa8MQMAEQzahLNaUHPEZR";

    private static final String TOKEN = "4715700322-RnW5THKzziFqJkQGDbtTlcpbdjKym9deLwyaeI5";

    private static final String TOKEN_SECRET = "bnj6pGlIgyQrPoMqdiWiCoBKLHpHSdiwQjz0QSa7xwsQA";

    /**
     * Add comma-separated list of terms, that should be searched for
     * e.g. "#Syria", "#trump"
     * Leave empty to not filter for specific terms
     */
    private static final List TRACK_TERMS = Arrays.asList("trump");

    /**
     * Add comma separated list of Twitter Account Ids, that should be followed
     * Transform Twitter handle into account id here: https://tweeterid.com
     * Leave empty to not follow specific accounts
     */
    private static final List FOLLOW_IDS = Arrays.asList();

    /**
     * DON'T TOUCH ME
     */
    private Client client;

    private final BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);

    private int retweets = 0;

    private int uniqueTweets = 0;

    private int impressions = 0;

    private static Long INTERVAL_IN_MS = 10000L;

    private long lastSendTimestamp = System.currentTimeMillis();

    @Before
    public void setup() {

        final Hosts twitterStreamHost = new HttpHosts(Constants.STREAM_HOST);
        final StatusesFilterEndpoint twitterEndpoint = new StatusesFilterEndpoint();
        if(TRACK_TERMS.size() > 0) {
            twitterEndpoint.trackTerms(TRACK_TERMS);
        }

        if(FOLLOW_IDS.size() > 0) {
            twitterEndpoint.followings(FOLLOW_IDS);
        }

        final Authentication oauth = new OAuth1(CONSUMER_KEY, CONSUMER_SECRET, TOKEN, TOKEN_SECRET);

        final ClientBuilder builder = new ClientBuilder()
                .hosts(twitterStreamHost)
                .authentication(oauth)
                .endpoint(twitterEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));

        client = builder.build();
        client.connect();
    }

    @Test
    public void draw() {
        while (!client.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.take();
                final Status status = TwitterObjectFactory.createStatus(msg);
                switch (EXTRACTED_DATA) {
                    case GEOLOCATION:
                        sendGeoLocation(status);
                        break;
                    case TWEET_STATISTICS:
                        sendRetweetCount(status);
                        break;
                    default:
                        System.out.println("You entered the wrong project type!");
                }

            } catch (Exception ex) {
            }
        }
    }

    private void sendGeoLocation(final Status status) {
        final Place place = status.getPlace();
        if(place != null) {
            final GeoLocation[][] coordinates = place.getBoundingBoxCoordinates();
            System.out.println("Message: " + status.getText());
            System.out.println("Country: " + place.getCountry());
            System.out.println("Coordinates: " + coordinates[0][0]);
        }
    }


    private void sendRetweetCount(final Status status) {
        // System.out.println(status.getText());
        long curTime = System.currentTimeMillis();

        if(!status.isRetweet()) {
            uniqueTweets++;
        } else {
            retweets++;
        }

        this.impressions += status.getUser().getFollowersCount();
        if((curTime - lastSendTimestamp) > INTERVAL_IN_MS) {

            System.out.println("UNIQUE: " + uniqueTweets);
            System.out.println("RETWEETS: " + retweets);
            System.out.println("IMPRESSIONS: " + impressions);

            // send data to Arduino
            uniqueTweets = 0;
            retweets = 0;
            impressions = 0;

            lastSendTimestamp = curTime;
        }
    }
}
