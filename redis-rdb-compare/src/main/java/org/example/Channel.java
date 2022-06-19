package org.example;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.messaging.PostUpdate.postTextResponseAsync;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.processing.Downloader;
import org.processing.Parser;
import org.processing.TrieMaker;
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
    @Getter
    private static final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>(); //static map of channel ids to channels

    @Builder.Default
    private final String s3linkA = "link-not-set-set";

    @Builder.Default
    private final String s3linkB = "link-not-set-yet";

    @Builder.Default
    private volatile String dumpA = "./.sessionFiles/dump-A-downloaded-notset.rdb";

    @Builder.Default
    private volatile String dumpB = "./.sessionFiles/dump-B-downloaded-notset.rdb";

    @Builder.Default
    private volatile String keysA = "./.sessionFiles/keys-A-notset.txt";

    @Builder.Default
    private volatile String keysB = "./.sessionFiles/keys-B-notset.txt";

    @NonNull
    private final String channelId;

    @Setter
    @Builder.Default
    private QTrie trieA = null;

    @Setter
    @Builder.Default
    private QTrie trieB = null;

    @Builder.Default
    private volatile Parser parser = Parser.builder().build();

    @Builder.Default
    private volatile TrieMaker trieMaker = TrieMaker.builder().build();

    @Builder.Default
    private volatile Downloader downloader = Downloader.builder().build();

    @Builder.Default
    private final String requestId = randomAlphanumeric(10);

    public static String formatLink(String s3link) {
        //TODO: fix this after getting url links
        return s3link.replace("https://", "");
    }

    private Channel setFileNames() {
        this.dumpA = "./.sessionFiles/dump-A-downloaded-" + this.getRequestId() + ".rdb";
        this.dumpB = "./.sessionFiles/dump-B-downloaded-" + this.getRequestId() + ".rdb";
        this.keysA = "./.sessionFiles/keys-A-" + this.getRequestId() + ".txt";
        this.keysB = "./.sessionFiles/keys-B-" + this.getRequestId() + ".txt";
        return this;
    }

    public enum DownloadingStatus {
        NOT_DOWNLOADED,
        DOWNLOADING,
        DOWNLOADED,
    }

    public enum ParsingStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
    }

    public enum TrieMakingStatus {
        NOT_CONSTRUCTED,
        CONSTRUCTING,
        CONSTRUCTED,
    }

    @Builder.Default
    @Setter
    private volatile DownloadingStatus downloadingStatus = DownloadingStatus.NOT_DOWNLOADED;

    @Builder.Default
    @Setter
    private volatile ParsingStatus parsingStatus = ParsingStatus.NOT_STARTED;

    @Builder.Default
    @Setter
    private volatile TrieMakingStatus trieMakingStatus = TrieMakingStatus.NOT_CONSTRUCTED;

    @Builder.Default
    private AtomicBoolean executedDownloading = new AtomicBoolean(false);

    @Builder.Default
    private AtomicBoolean executedParsing = new AtomicBoolean(false);

    @Builder.Default
    private AtomicBoolean executedTrieMaking = new AtomicBoolean(false);

    @Builder.Default
    @Setter
    private volatile long downloadingTime = -1;

    @Builder.Default
    @Setter
    private volatile long parsingTime = -1;

    @Builder.Default
    @Setter
    private volatile long trieMakingTime = -1;

    @Builder.Default
    private ExecutorService trieMakingExecutorService = SingleNameableExecutorService
        .builder()
        .baseName("make-trie-caller")
        .build()
        .getExecutorService();

    @Builder.Default
    private ExecutorService parsingExecutorService = SingleNameableExecutorService
        .builder()
        .baseName("parsing-caller")
        .build()
        .getExecutorService();

    @Builder.Default
    private ExecutorService downloadingExecutorService = SingleNameableExecutorService
        .builder()
        .baseName("downloading-caller")
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
        Channel channel = channels.putIfAbsent(
            channelId,
            Channel.builder().channelId(channelId).build().setFileNames()
        );
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
}
