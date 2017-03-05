package cyberspace.modulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import cyberspace.modulator.config.CyberSpaceModulatorConfig;
import cyberspace.modulator.config.ExtractedData;
import cyberspace.modulator.config.TwitterConfig;
import interfascia.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processing.core.PApplet;
import processing.serial.Serial;
import twitter4j.GeoLocation;
import twitter4j.Place;
import twitter4j.Status;
import twitter4j.TwitterObjectFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CyberSpaceModulatorApplet extends PApplet {

    /**
     * Used for input GUI
     */
    private GUIController guiController;
    private IFTextField textField;
    private IFLabel label;
    private IFLabel label2;
    private IFButton b1;
    private IFButton b2;

    /**
     * Change this depending on your project
     */
    private static final ExtractedData EXTRACTED_DATA = ExtractedData.TWEET_STATISTICS;
    private static final Long INTERVAL_IN_MS = 10000L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CyberSpaceModulatorApplet.class);
    private final BlockingQueue<String> msgQueue = new LinkedBlockingQueue<>(100000);
    private final String configPath = "resources/config.json";


    /**
     * DON'T TOUCH ME
     */
    private Client client;
    private Serial myPort;
    private int retweets = 0;
    private int uniqueTweets = 0;
    private int impressions = 0;
    private long lastSendTimestamp = System.currentTimeMillis();
    private String tweetvar = "";
    private CyberSpaceModulatorConfig config = null;

    @Override
    public void settings() {
        size(1000, 1000);
    }

    @Override
    public void setup() {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            this.config = mapper.readValue(Paths.get(this.configPath).toFile(), CyberSpaceModulatorConfig.class);
        } catch (final IOException e) {
            LOGGER.error("Unable to read configuration", e);
            return;
        }

        // only create GUI if we actually want to use it
        if (this.config.getSetUpGui()) {

            this.guiController = new GUIController(this);
            this.textField = new IFTextField("Text Field", 25, 30, 150);  // x, y, length
            this.label = new IFLabel("Listen to: ", 25, 60);
            this.label2 = new IFLabel(this.tweetvar, 80, 60);

            this.b1 = new IFButton("Send", 200, 30, 40, 20);
            this.b2 = new IFButton("Reset", 250, 30, 40, 20);

            this.guiController.add(this.textField);
            this.guiController.add(this.label);
            this.guiController.add(this.label2);

            this.guiController.add(this.b1);
            this.guiController.add(this.b2);

            this.guiController.addActionListener(this);
            this.b1.addActionListener(this);
            this.b2.addActionListener(this);
        }


        if (this.config.getSendToArduino()) {
            // List all the available serial ports:
            LOGGER.info(Serial.list().toString());

            // Open the port you are using at the rate you want:
            // Use USB-Port as Serial [x]
            this.myPort = new Serial(this, Serial.list()[0], 9600);
        }

        final HttpHosts twitterStreamHost = new HttpHosts(Constants.STREAM_HOST);
        final StatusesFilterEndpoint twitterEndpoint = new StatusesFilterEndpoint();
        final List<String> trackTerms = this.config.getTrackTerms();
        if (trackTerms != null && trackTerms.size() > 0) {
            twitterEndpoint.trackTerms(trackTerms);
        }

        final List<Long> followIds = this.config.getFollowIds();
        if (followIds != null && followIds.size() > 0) {
            twitterEndpoint.followings(followIds);
        }

        final TwitterConfig twitterConfig = this.config.getTwitterConfig();
        final Authentication oauth = new OAuth1(twitterConfig.getConsumerKey(), twitterConfig.getConsumerSecret(), twitterConfig.getToken(), twitterConfig.getTokenSecret());

        final ClientBuilder builder = new ClientBuilder()
                .hosts(twitterStreamHost)
                .authentication(oauth)
                .endpoint(twitterEndpoint)
                .processor(new StringDelimitedProcessor(this.msgQueue));

        this.client = builder.build();
        this.client.connect();

    }

    @Override
    public void draw() {
        background(200);
        String msg = null;
        try {
            msg = this.msgQueue.take();
            final Status status = TwitterObjectFactory.createStatus(msg);
            switch (EXTRACTED_DATA) {
                case GEOLOCATION:
                    sendGeoLocation(status);
                    break;
                case TWEET_STATISTICS:
                    sendRetweetCount(status);
                    break;
                default:
                    LOGGER.error("Project type " + status + " is not supported.");
            }

        } catch (final Exception ex) {
            LOGGER.error("An unexpected error occured", ex);
        }
    }

    private void sendGeoLocation(final Status status) {
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
            final String coordinatesStr = ("lat" + rndLati + "long" + rndLongi + "\n");
            LOGGER.debug(coordinatesStr);

            if (this.config.getSendToArduino()) {
                sendToArduino(coordinatesStr);
            }
        }
    }

    private void sendToArduino(final String coordinatesStr) {
        // Send string to serial
        this.myPort.write(coordinatesStr);
        // wait for 100ms
        delay(100);
    }


    private void sendRetweetCount(final Status status) {

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
            LOGGER.debug(tweetStatistics);

            // send data to Arduino
            sendToArduino(tweetStatistics);

            this.uniqueTweets = 0;
            this.retweets = 0;
            this.impressions = 0;

            this.lastSendTimestamp = curTime;
        }
    }

    void actionPerformed(final GUIEvent e) {
        LOGGER.debug("Component: " + e.getSource()); //.getLabel());
        LOGGER.debug("Message: " + e.getMessage());

        if (e.getMessage().equals("Completed")) {
            this.label.setLabel(this.textField.getValue());
            this.tweetvar = this.textField.getValue();
            LOGGER.debug("tweetvar: " + this.tweetvar);
        }

        if (e.getSource() == this.b1) {
            this.label.setLabel(this.textField.getValue());
            this.tweetvar = this.textField.getValue();
            LOGGER.debug("tweetvar: " + this.tweetvar);
        } else if (e.getSource() == this.b2) {
            this.textField.setValue("");
            this.label.setLabel("");
            this.tweetvar = "";
            LOGGER.debug("tweetvar: " + this.tweetvar);
        }
    }
}

