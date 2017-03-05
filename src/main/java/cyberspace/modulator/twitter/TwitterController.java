package cyberspace.modulator.twitter;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import cyberspace.modulator.config.CyberSpaceModulatorConfig;
import cyberspace.modulator.config.TwitterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.*;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.StrictMath.round;

public class TwitterController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterController.class);

    private int retweets = 0;

    private int uniqueTweets = 0;

    private int impressions = 0;

    private long lastSendTimestamp = System.currentTimeMillis();

    private static final Long INTERVAL_IN_MS = 10000L;

    private final BlockingQueue<String> msgQueue = new LinkedBlockingQueue<>(100000);

    private final Client client;

    public TwitterController(final CyberSpaceModulatorConfig config) {
        final HttpHosts twitterStreamHost = new HttpHosts(Constants.STREAM_HOST);
        final StatusesFilterEndpoint twitterEndpoint = new StatusesFilterEndpoint();
        final List<String> trackTerms = config.getTrackTerms();
        if (trackTerms != null && trackTerms.size() > 0) {
            twitterEndpoint.trackTerms(trackTerms);
        }

        final List<Long> followIds = config.getFollowIds();
        if (followIds != null && followIds.size() > 0) {
            twitterEndpoint.followings(followIds);
        }

        final TwitterConfig twitterConfig = config.getTwitterConfig();
        final Authentication oauth = new OAuth1(twitterConfig.getConsumerKey(), twitterConfig.getConsumerSecret(), twitterConfig.getToken(), twitterConfig.getTokenSecret());

        final ClientBuilder builder = new ClientBuilder()
                .hosts(twitterStreamHost)
                .authentication(oauth)
                .endpoint(twitterEndpoint)
                .processor(new StringDelimitedProcessor(this.msgQueue));

        this.client = builder.build();
        this.client.connect();
    }

    /**
     * Retrieve next status from TwitterController
     * Blocks, until next message has been retrieved
     *
     * @return Next Twitter status
     * @throws InterruptedException, TwitterException
     */
    public Status getStatus() throws InterruptedException, TwitterException {
        final String message = this.msgQueue.take();
        return TwitterObjectFactory.createStatus(message);
    }

    public String computeGeoLocation(final Status status) {
        final Place place = status.getPlace();
        if (place != null) {
            final GeoLocation[][] coordinates = place.getBoundingBoxCoordinates();
            LOGGER.debug("Message: " + status.getText());
            LOGGER.debug("Country: " + place.getCountry());
            LOGGER.debug("Coordinates: " + coordinates[0][0]);

            // latitude from 0 to 180
            final double lati = coordinates[0][0].getLatitude() + 90;
            // longitude from 0 to 360
            final double longi = coordinates[0][0].getLongitude() + 180;

            final int rndLati = round((float) lati);
            final int rndLongi = round((float) longi);

            // create string that is sent to Arduino
            return ("lat" + rndLati + "long" + rndLongi + "\n");
        }
        return null;
    }


    public String computeRetweetCount(final Status status) {

        final long curTime = System.currentTimeMillis();

        if (!status.isRetweet()) {
            this.uniqueTweets++;
        } else {
            this.retweets++;
        }

        this.impressions += status.getUser().getFollowersCount();
        if ((curTime - this.lastSendTimestamp) > INTERVAL_IN_MS) {

            LOGGER.debug("UNIQUE: " + this.uniqueTweets);
            LOGGER.debug("RETWEETS: " + this.retweets);
            LOGGER.debug("IMPRESSIONS: " + this.impressions);

            final String tweetStatistics = ("uniquetweets" + this.uniqueTweets + "retweets" + this.retweets + "impressions" + this.impressions + "\n");      // String basteln

            this.uniqueTweets = 0;
            this.retweets = 0;
            this.impressions = 0;

            this.lastSendTimestamp = curTime;
            return tweetStatistics;
        }
        return null;
    }
}
