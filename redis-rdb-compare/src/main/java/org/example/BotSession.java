package org.example;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.messaging.PostUpdate.postTextResponseAsync;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.processing.Downloader;
import org.processing.Parser;
import org.processing.TrieMaker;
import org.threading.SingleNameableExecutorService;
import org.trie.QTrie;

/**
 * BotSession Class
 * Maps a botSession id to dumps files, keys files, a parser and tries and maintains their status.
 */
@Slf4j
@Getter
@Builder
public class BotSession implements AutoCloseable {

    @Builder.Default
    @Getter
    private static final ConcurrentHashMap<String, BotSession> botSessions = new ConcurrentHashMap<>(); //static map of botSession ids to requestIds

    @Setter
    private URL s3linkA;

    @Setter
    private URL s3linkB;

    @Builder.Default
    private volatile String dumpA = "./.sessionFiles/dump-A-downloaded-notset.rdb";

    @Builder.Default
    private volatile String dumpB = "./.sessionFiles/dump-B-downloaded-notset.rdb";

    @Builder.Default
    private volatile String keysA = "./.sessionFiles/keys-A-notset.txt";

    @Builder.Default
    private volatile String keysB = "./.sessionFiles/keys-B-notset.txt";

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

    private final String requestId;

    public static String formatLink(String s3link) {
        if (s3link == null) {
            return "link-not-set-yet";
        }
        //TODO: fix this after getting url links
        return s3link.replace("https://", "");
    }

    private BotSession setFileNames() {
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

    /**
     * Getter for the botSession.
     *
     * @param requestId: the id for the required botSession
     * @return BotSession object
     */
    public static BotSession getBotSession(final String requestId) throws IllegalStateException {
        if (!botSessions.containsKey(requestId)) {
            postTextResponseAsync(
                "Sorry, you need to create a session first by running \"/process\"",
                requestId
            );
            // TODO: what to do here
            log.error("requested botSession by id " + requestId + " does not exist");
            throw new IllegalStateException(
                "requested botSession by id " + requestId + " does not exist"
            );
        }
        return botSessions.get(requestId);
    }

    /**
     * Setter for the botSession.
     *
     * @return true when new botSession is created, otherwise false
     */
    public static String createBotSession() {
        String requestId = randomAlphanumeric(10);
        BotSession botSession = botSessions.putIfAbsent(
            requestId,
            BotSession.builder().requestId(requestId).build().setFileNames()
        );
        return (botSession == null) ? requestId : null;
    }

    /**
     * Remove a botSession from the botSessions map.
     *
     * @param requestId: the id for the botSession to be removed
     */
    public static void removeBotSession(final String requestId) {
        log.info("removeBotSession() called");
        botSessions.remove(requestId);
    }

    /**
     * Implements AutoCloseable interface method close().
     */
    @Override
    public void close() {
        log.info("AutoClose() called");
    }
}
