package org.example;

import static org.messaging.PostUpdate.postTextResponseAsync;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.processing.Parser;
import org.threading.SingleNameableExecutorService;
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
    private final String dumpA = "../dump-A-200M.rdb";

    @Builder.Default
    private final String dumpB = "../dump-B-200M.rdb";

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

    public enum FileStatus {
        NOT_DOWNLOADED,
        DOWNLOADING,
        DOWNLOADED,
    }

    @Builder.Default
    @Setter
    private volatile ParsingStatus parsingStatus = ParsingStatus.NOT_STARTED;

    @Builder.Default
    @Setter
    private volatile TrieStatus trieStatus = TrieStatus.NOT_CONSTRUCTED;

    @Builder.Default
    @Setter
    private volatile FileStatus fileStatus = FileStatus.DOWNLOADED; //TODO: change this later

    @Builder.Default
    private AtomicBoolean executedParsing = new AtomicBoolean(false);

    @Builder.Default
    private AtomicBoolean executedTrie = new AtomicBoolean(false);

    @Builder.Default
    @Setter
    private volatile long parsingTime = -1;

    @Builder.Default
    @Setter
    private volatile long makeTrieTime = -1;

    @Builder.Default
    private ExecutorService makeTrieExecutorService = SingleNameableExecutorService
        .builder()
        .baseName("make-trie-thread")
        .build()
        .getExecutorService();

    @Builder.Default
    private ExecutorService parsingExecutorService = SingleNameableExecutorService
        .builder()
        .baseName("parsing-thread")
        .build()
        .getExecutorService();

    public static void printChannels() {
        log.info("printing...");
        for (String channelId : channels.keySet()) {
            log.info("Channel {}", channelId);
        }
    }

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
            log.error("requested channel by id " + channelId + " does not exist");
            throw new IllegalStateException(
                "requested channel by id " + channelId + " does not exist"
            );
        }
        return channels.get(channelId);
    }

    /**
     * Setter for the channel.
     * @param channelId: the id for the required channel
     * @return true when new channel is created, otherwise false
     */
    public static boolean createChannel(final String channelId) {
        Channel channel = channels.putIfAbsent(channelId, Channel.builder().build());
        return (channel == null);
    }

    /**
     * Remove a channel from the channels map.
     * @param channelId: the id for the channel to be removed
     */
    public static void removeChannel(final String channelId) {
        log.info("removeChannel() called");
        channels.remove(channelId);
    }

    /**
     * Reset the data of a channel to default values.
     * - Delete data files (.rdb) from drive. Reset dumpA, dumpB.
     * - Delete keys files (.txt) from drive. Reset keysA, keysB.
     * - Reset S3 links.
     * - Reset parsing status.
     * - Reset trie status.
     * - Reset file status.
     * - Reset trieA, trieB.
     * - Reset parser
     * - Reset executedParsing, executedTrie
     * - Reset parsingTime, makeTrieTime
     */
    public void resetChannel() {
        log.info("reset() called on this session");
    }
}
