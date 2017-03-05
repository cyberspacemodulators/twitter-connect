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
import processing.core.PApplet;
import processing.serial.Serial;
import twitter4j.GeoLocation;
import twitter4j.Place;
import twitter4j.Status;
import twitter4j.TwitterObjectFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CyberSpaceModulatorApplet extends PApplet {

    /** Used for input GUI */
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


    /**
     * Add comma-separated list of terms, that should be searched for
     * e.g. "#Syria", "#trump"
     * Leave empty to not filter for specific terms
     */
   // private static final List TRACK_TERMS = Arrays.asList("trump");

    /**
     * Add comma separated list of Twitter Account Ids, that should be followed
     * Transform Twitter handle into account id here: https://tweeterid.com
     * Leave empty to not follow specific accounts
     */
    // private static final List FOLLOW_IDS = Arrays.asList();

    /**
     * DON'T TOUCH ME
     */
    private Client client;

    private Serial myPort;

    private final BlockingQueue<String> msgQueue = new LinkedBlockingQueue<>(100000);

    private int retweets = 0;

    private int uniqueTweets = 0;

    private int impressions = 0;

    private static final Long INTERVAL_IN_MS = 10000L;

    private long lastSendTimestamp = System.currentTimeMillis();

    private String tweetvar = "";

    private String configPath = "resources/config.json";

    @Override
    public void settings() {
        size(1000, 1000);

    }

    @Override
    public void setup() {

        guiController = new GUIController(this);
        textField = new IFTextField("Text Field", 25, 30, 150);  // x, y, length
        label = new IFLabel("Listen to: ", 25, 60);
        label2 = new IFLabel(tweetvar, 80, 60);

        b1 = new IFButton("Send", 200, 30, 40, 20);
        b2 = new IFButton("Reset", 250, 30, 40, 20);

        guiController.add(textField);
        guiController.add(label);
        guiController.add(label2);

        guiController.add(b1);
        guiController.add(b2);

        guiController.addActionListener(this);
        b1.addActionListener(this);
        b2.addActionListener(this);

        // List all the available serial ports:
        printArray(Serial.list());

        // Open the port you are using at the rate you want:
        // myPort = new Serial(this, Serial.list()[0], 9600);      // USB-Port als Serial benutzen [x] Zahl ändern

        final ObjectMapper mapper = new ObjectMapper();
        final CyberSpaceModulatorConfig config;
        try {
            config = mapper.readValue(Paths.get(configPath).toFile(), CyberSpaceModulatorConfig.class);
        } catch (IOException e) {
            // TODO: Replace all outputs with logger calls
            e.printStackTrace();
            return;
        }


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
            .processor(new StringDelimitedProcessor(msgQueue));

        client = builder.build();
        client.connect();

    }

    @Override
    public void draw() {
        background(200);

        // TODO: make me work again
        // while (!client.isDone()) {
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

        } catch (final Exception ex) {
        }
    }

    //}

    private void sendGeoLocation(final Status status) {
        final Place place = status.getPlace();
        if (place != null) {
            final GeoLocation[][] coordinates = place.getBoundingBoxCoordinates();
            System.out.println("Message: " + status.getText());
            System.out.println("Country: " + place.getCountry());
            System.out.println("Coordinates: " + coordinates[0][0]);

            // TODO: Comments should be English
            final double lati = coordinates[0][0].getLatitude() + 90;       // latitude von 0 bis 180
            final double longi = coordinates[0][0].getLongitude() + 180;    // longitude von 0 bis 360

            final int rndLati = round((float) lati);                       // Runden auf volle Zahl
            final int rndLongi = round((float) longi);                     // Runden auf volle Zahl

            final String coordinatesStr = ("lat" + rndLati + "long" + rndLongi + "\n");      // String basteln
            println(coordinatesStr);                                                   // String in Konsole anzeigen

            myPort.write(coordinatesStr);                  // String an Serial schicken
            delay(100);                                    // 100ms pause
        }
    }

    private void sendRetweetCount(final Status status) {
        // System.out.println(status.getText());
        final long curTime = System.currentTimeMillis();

        if (!status.isRetweet()) {
            uniqueTweets++;
        } else {
            retweets++;
        }

        this.impressions += status.getUser().getFollowersCount();
        if ((curTime - lastSendTimestamp) > INTERVAL_IN_MS) {

            System.out.println("UNIQUE: " + uniqueTweets);
            System.out.println("RETWEETS: " + retweets);
            System.out.println("IMPRESSIONS: " + impressions);

            // send data to Arduino

            final String tweetStatistics = ("uniquetweets" + uniqueTweets + "retweets" + retweets + "impressions" + impressions + "\n");      // String basteln
            println(tweetStatistics);                                                   // String in Konsole anzeigen

            //myPort.write(tweetStatistics);                  // String an Serial schicken
            // delay(100);                                    // 100ms pause

            uniqueTweets = 0;
            retweets = 0;
            impressions = 0;

            lastSendTimestamp = curTime;
        }
    }

    void actionPerformed(final GUIEvent e) {
        // println ("Component: " + e.getSource()); //.getLabel());
        // println ("Message: " + e.getMessage());

        if (e.getMessage().equals("Completed")) {
            label.setLabel(textField.getValue());
            tweetvar = textField.getValue();
            println("tweetvar: " + tweetvar);
        }

        if (e.getSource() == b1) {
            label.setLabel(textField.getValue());
            tweetvar = textField.getValue();
            println("tweetvar: " + tweetvar);
        } else if (e.getSource() == b2) {
            textField.setValue("");
            label.setLabel("");
            tweetvar = "";
            println("tweetvar: " + tweetvar);
        }
    }
}

