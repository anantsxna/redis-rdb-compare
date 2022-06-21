package org.example;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.messaging.PostUpdate.postTextResponseAsync;

import java.net.MalformedURLException;
import java.net.URL;
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
 * BotSession Class
 * Maps a botSession id to dumps files, keys files, a parser and tries and maintains their status.
 */
@Slf4j
@Getter
@Builder
public class BotSession {

    @Builder.Default
    @Getter
    private static final ConcurrentHashMap<String, BotSession> allBotSessions = new ConcurrentHashMap<>(); //static map of botSession ids to requestIds

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

    public static String elongateURL(@NonNull String s3link) {
        if (!s3link.matches("^\\w+?://.*")) {
            s3link = "https://" + s3link;
        }
        return s3link;
    }

    private BotSession setFileNames() {
        this.dumpA = "./.sessionFiles/dump-A-downloaded-" + this.getRequestId() + ".rdb";
        this.dumpB = "./.sessionFiles/dump-B-downloaded-" + this.getRequestId() + ".rdb";
        this.keysA = "./.sessionFiles/keys-A-" + this.getRequestId() + ".txt";
        this.keysB = "./.sessionFiles/keys-B-" + this.getRequestId() + ".txt";
        try {
            this.s3linkA = new URL("https://example.com");
            this.s3linkB = new URL("https://example.com");
        } catch (MalformedURLException e) {
            log.error("Failed to initialize s3links for botSession: {}", this.getRequestId());
            e.printStackTrace();
        }
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
        if (!allBotSessions.containsKey(requestId)) {
            postTextResponseAsync(
                "Sorry, you need to create a session first by running \"/process\"",
                requestId
            );
            log.error("requested botSession by id " + requestId + " does not exist");
            throw new IllegalStateException(
                "requested botSession by id " + requestId + " does not exist"
            );
        }
        return allBotSessions.get(requestId);
    }

    /**
     * Setter for the botSession. Synchronised so that 2 sessions don't get the same id
     *
     * @return true when new botSession is created, otherwise false
     */
    public static synchronized String createBotSession() {
        String requestId = "#" + randomNumeric(6);
        BotSession botSession = allBotSessions.putIfAbsent(
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
        allBotSessions.remove(requestId);
    }

    /**
     * initiate the downloading of the dump files.
     */
    public String initiateDownloading() throws InterruptedException {
        log.info("initiateDownload() called for bot session " + this.getRequestId());
        this.setDownloadingStatus(DownloadingStatus.DOWNLOADING);
        Downloader downloader = this.getDownloader();
        downloader.addToDownloader(this.getS3linkA(), this.getDumpA());
        downloader.addToDownloader(this.getS3linkB(), this.getDumpB());

        long startTime = System.currentTimeMillis();
        try {
            boolean terminatedWithSuccess = this.getDownloader().download();
            if (!terminatedWithSuccess) {
                throw new InterruptedException("Timeout Exception while downloading");
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            log.error(
                "download interrupted due to FileNotFound, SecurityException or DownloadingError for botSession {}",
                requestId
            );
            this.setDownloadingStatus(DownloadingStatus.NOT_DOWNLOADED);
            throw new InterruptedException(
                "\uD83D\uDEA8\uD83D\uDEA8 Download failed \uD83D\uDEA8\uD83D\uDEA8"
            );
        }
        long endTime = System.currentTimeMillis();
        log.info(
            "Download completed in {} milliseconds in botSession {}",
            endTime - startTime,
            requestId
        );
        this.setDownloadingTime(endTime - startTime);
        this.setDownloadingStatus(DownloadingStatus.DOWNLOADED); //volatile variable write
        return (
            "\uD83D\uDEA8\uD83D\uDEA8 Downloading completed in " +
            (endTime - startTime) /
            1000.0 +
            " second(s). \uD83D\uDEA8\uD83D\uDEA8"
        );
    }

    /**
     * initiate the parsing of the dump files.
     */
    public String initiateParsing() throws InterruptedException {
        log.info("parsing started for botSession {}", getRequestId());
        this.setParsingStatus(ParsingStatus.IN_PROGRESS);
        Parser parser = this.getParser();
        parser.addToParser(this.getDumpA(), this.getKeysA());
        parser.addToParser(this.getDumpB(), this.getKeysB());

        long startTime = System.currentTimeMillis();
        parser.parse();
        this.setParsingStatus(ParsingStatus.COMPLETED); //volatile variable write
        long endTime = System.currentTimeMillis();
        log.info(
            "parsing completed for botSession {} in {} ms",
            getRequestId(),
            endTime - startTime
        );
        this.setParsingTime(endTime - startTime);
        return (
            "\uD83D\uDEA8\uD83D\uDEA8 Parsing completed in " +
            (endTime - startTime) /
            1000.0 +
            " second(s). \uD83D\uDEA8\uD83D\uDEA8"
        );
    }

    /**
     * initiate the trie construction.
     */
    public String initiateTrieMaking() throws InterruptedException {
        this.setTrieMakingStatus(TrieMakingStatus.CONSTRUCTING);
        log.info("trie construction started for botSession {}", requestId);
        this.setTrieA(QTrie.builder().keysFile(this.getKeysA()).build());
        this.setTrieB(QTrie.builder().keysFile(this.getKeysB()).build());

        this.getTrieMaker().addToTrieMaker(this.getDumpA(), this.getTrieA());
        this.getTrieMaker().addToTrieMaker(this.getDumpB(), this.getTrieB());

        long startTime = System.currentTimeMillis();
        try {
            boolean terminatedWithSuccess = this.getTrieMaker().makeTries();
            if (!terminatedWithSuccess) {
                throw new Exception("Exception while making tries");
            }
        } catch (InterruptedException e) {
            log.error(
                "trie construction interrupted due trie-initializer-threads being interrupted for botSession {}",
                this.getRequestId()
            );
            this.setTrieMakingStatus(TrieMakingStatus.NOT_CONSTRUCTED);
            throw new InterruptedException(
                "\uD83D\uDEA8\uD83D\uDEA8 Trie construction failed due trie-initializer-threads being interrupted for botSession\uD83D\uDEA8\uD83D\uDEA8"
            );
        } catch (Exception e) {
            log.error(
                "trie construction interrupted due to timeout for botSession {}",
                this.getRequestId()
            );
            this.setTrieMakingStatus(TrieMakingStatus.NOT_CONSTRUCTED);
            throw new InterruptedException(
                "\uD83D\uDEA8\uD83D\uDEA8 Trie construction failed due to timeout for botSession \uD83D\uDEA8\uD83D\uDEA8"
            );
        }
        long endTime = System.currentTimeMillis();
        log.info(
            "Trie construction completed in {} milliseconds in botSession {}",
            endTime - startTime,
            this.getRequestId()
        );
        this.setTrieMakingTime(endTime - startTime);
        this.setTrieMakingStatus(TrieMakingStatus.CONSTRUCTED); //volatile variable write
        return (
            "\uD83D\uDEA8\uD83D\uDEA8 Trie construction completed in " +
            (endTime - startTime) /
            1000.0 +
            " second(s). \uD83D\uDEA8\uD83D\uDEA8"
        );
    }
}
