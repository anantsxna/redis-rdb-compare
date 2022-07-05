package org.example;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class SlackUtilsTest {

    public static Properties props = new Properties();

    static {
        try {
            props.load(
                Thread
                    .currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("application.properties")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createSessionTest() {
        String requestId = "#" + randomNumeric(6);
        log.info("requestId: {}", requestId);
        final BotSession botSession = BotSession
            .builder()
            .requestId(requestId)
            .build()
            .setFileNames();
        assertNotNull(requestId);
        assertNotNull(botSession);
    }
}
