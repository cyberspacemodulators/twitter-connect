package cyberspace.modulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import cyberspace.modulator.config.CyberSpaceModulatorConfig;
import cyberspace.modulator.config.ExtractedData;
import cyberspace.modulator.twitter.TwitterController;
import interfascia.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processing.core.PApplet;
import processing.serial.Serial;
import twitter4j.Status;

import java.io.IOException;
import java.nio.file.Paths;

public class CyberSpaceModulatorApplet extends PApplet {

    private static final Logger LOGGER = LoggerFactory.getLogger(CyberSpaceModulatorApplet.class);

    private static final String configPath = "resources/config.json";

    /**
     * Change this depending on your project
     */
    private final ExtractedData extractedData = null;

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
     * DON'T TOUCH ME
     */
    private Serial myPort;

    private CyberSpaceModulatorConfig config = null;

    private String tweetvar = "";

    private TwitterController twitterController;

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

        this.twitterController = new TwitterController(this.config);

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

    }

    @Override
    public void draw() {
        background(200);
        try {
            final Status status = this.twitterController.getStatus();
            String result = null;
            switch (this.extractedData) {
                case GEOLOCATION:
                    result = this.twitterController.computeGeoLocation(status);
                    break;
                case TWEET_STATISTICS:
                    result = this.twitterController.computeRetweetCount(status);
                    break;
                default:
                    LOGGER.error("Project type " + status + " is not supported.");
            }
            if (result != null) {
                LOGGER.debug(result);
            }

            if (this.config.getSendToArduino() && result != null) {
                sendToArduino(result);
            }
        } catch (final Exception ex) {
            LOGGER.error("An unexpected error occured", ex);
        }
    }


    private void sendToArduino(final String coordinatesStr) {
        // Send string to serial
        this.myPort.write(coordinatesStr);
        // wait for 100ms
        delay(100);
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

