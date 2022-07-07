package org.trie;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class QTrieTest {

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

    //    @Test
    //    public void checkDelimiter() {
    //        assertEquals(props.getProperty("DELIMITER"), ":");
    //        QTrie.setDELIMITER(props.getProperty("DELIMITER"));
    //        assertEquals(QTrie.getDELIMITER(), ":");
    //    }
    //    @Test
    //    public void initTest() {
    //        QTrie trie = QTrie.builder().keysFile("./src/test/resources/testKeysFile.txt").build();
    //        assertEquals(trie.getRoot().getCount(), 0);
    //        trie.takeInput();
    //        //        trie.insertKey("hello:bye");
    //        assertEquals(trie.getRoot().getCount(), 7956);
    //    }

    @Test
    public void isS3() {
        try {
            new URL("s3://qa4-cdata-app.sprinklr.com/EXPORT/itops-348637-001-node-0001.rdb");
        } catch (Exception e) {
            log.info("Not an S3 URL");
        }
    }
}
