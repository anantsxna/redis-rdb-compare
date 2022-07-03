package org.example;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileInputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class SlackUtilsTest {

    private static final String rootPath = Thread
        .currentThread()
        .getContextClassLoader()
        .getResource("")
        .getPath();
    private static final String appConfigPath = rootPath + "application.properties";

    public static Properties props = new Properties();

    static {
        try {
            props.load(new FileInputStream(appConfigPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createSessionTest() {
        String requestId = "#" + randomNumeric(6);
        final BotSession botSession = BotSession
            .builder()
            .requestId(requestId)
            .build()
            .setFileNames();
        assertNotNull(requestId);
    }
}
