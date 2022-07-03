package org.trie;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileInputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class QTrieTest {

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
    public void checkDelimiter() {
        assertEquals(props.getProperty("DELIMITER"), ":");
        QTrie.setDELIMITER(props.getProperty("DELIMITER"));
        assertEquals(QTrie.getDELIMITER(), ":");
    }

    @Test
    public void initTest() {
        QTrie trie = QTrie.builder().keysFile("./src/test/resources/testKeysFile.txt").build();
        assertEquals(trie.getRoot().getCount(), 0);
        trie.takeInput();
        //        trie.insertKey("hello:bye");
        assertEquals(trie.getRoot().getCount(), 7956);
    }
}
