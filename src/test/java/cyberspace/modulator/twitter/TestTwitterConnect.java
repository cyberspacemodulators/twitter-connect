package cyberspace.modulator.twitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import cyberspace.modulator.config.CyberSpaceModulatorConfig;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Status;
import twitter4j.TwitterException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.stream.IntStream;

public class TestTwitterConnect {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestTwitterConnect.class);

    // TODO: Provide path to your own configuration file
    private static final String CONFIG_PATH = "src/test/resources/config.json";

    @Test
    public void testApplet() {

        final CyberSpaceModulatorConfig config = getConfig();
        Assert.assertNotNull(config);
        final TwitterController controller = new TwitterController(config);

        // retrieve 10 messages and print them
        IntStream.range(0, 10).forEach(m -> {
            try {
                final Status status = controller.getStatus();
                LOGGER.info(status.toString());
            } catch (final InterruptedException | TwitterException e) {
                LOGGER.error("Could not retrieve message from Twitter Controller");
                Assert.fail();
            }
        });
    }

    private CyberSpaceModulatorConfig getConfig() {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(Paths.get(CONFIG_PATH).toFile(), CyberSpaceModulatorConfig.class);
        } catch (final IOException e) {
            LOGGER.error("Unable to read configuration", e);
            return null;
        }
    }
}
