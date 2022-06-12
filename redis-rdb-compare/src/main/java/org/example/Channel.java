package org.example;

import java.util.HashMap;
import org.processing.Parser;
import org.trie.QTrie;

public class Channel {

    static volatile HashMap<String, Channel> channels = new HashMap<>();

    public String dumpA = "../dump-A.rdb";
    public String dumpB = "../dump-B.rdb";
    public String keysA = "../keys-A.txt";
    public String keysB = "../keys-B.txt";
    public QTrie trieA = null;
    public QTrie trieB = null;

    public Parser parser = new Parser();

    public enum ParsingStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
    }

    public enum TrieStatus {
        NOT_CONSTRUCTED,
        CONSTRUCTING,
        CONSTRUCTED,
    }

    public volatile ParsingStatus parsingStatus = ParsingStatus.NOT_STARTED;
    public volatile TrieStatus trieStatus = TrieStatus.NOT_CONSTRUCTED;

    public Channel() {}

    public static Channel getChannel(String channelId) {
        if (!channels.containsKey(channelId)) {
            channels.put(channelId, new Channel());
        }
        return channels.get(channelId);
    }
}
