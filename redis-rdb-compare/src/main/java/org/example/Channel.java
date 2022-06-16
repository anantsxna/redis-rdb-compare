package org.example;

import static org.messaging.PostUpdate.postTextResponseAsync;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.processing.Parser;
import org.trie.QTrie;

/**
 * Channel Class
 * Maps a channel id to dumps files, keys files, a parser and tries and maintains their status.
 */
@Slf4j
@Getter
@Builder
public class Channel {

    @Builder.Default
    private static final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>(); //static map of channel ids to channels

    @Builder.Default
    private final String dumpA = "../dump-A.rdb";

    @Builder.Default
    private final String dumpB = "../dump-B.rdb";

    @Builder.Default
    private final String keysA = "../keys-A.txt";

    @Builder.Default
    private final String keysB = "../keys-B.txt";

    @Setter
    @Builder.Default
    private QTrie trieA = null;

    @Setter
    @Builder.Default
    private QTrie trieB = null;

    @Builder.Default
    private volatile Parser parser = Parser.builder().build();

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

    @Builder.Default
    @Setter
    private volatile ParsingStatus parsingStatus = ParsingStatus.NOT_STARTED;

    @Builder.Default
    @Setter
    public volatile TrieStatus trieStatus = TrieStatus.NOT_CONSTRUCTED;

    @Builder.Default
    @Getter
    private ReadWriteLock parseLock = new ReentrantReadWriteLock();

    /**
     * Getter for the channel.
     * @param channelId: the id for the required channel
     * @return Channel object
     */
    public static Channel getChannel(final String channelId) throws IllegalStateException {
        if (!channels.containsKey(channelId)) {
            postTextResponseAsync(
                "Sorry, you need to create a session first by running \"/process\"",
                channelId
            );
            // TODO: what to do here
            //            throw new IllegalStateException(
            //                "requested channel by id " + channelId + " does not exist"
            //            );
            return null;
        }
        return channels.get(channelId);
    }

    /**
     * Setter for the channel.
     * @param channelId: the id for the required channel
     * @return whether channel already exists or not
     */
    public static boolean createChannel(final String channelId) {
        if (channels.containsKey(channelId)) {
            return false;
        }
        channels.put(channelId, Channel.builder().build());
        return true;
    }

    /**
     * Remove a channel from the channels map.
     * @param channelId: the id for the channel to be removed
     */
    public static void removeChannel(final String channelId) {
        channels.remove(channelId);
    }
}
