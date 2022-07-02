package org.example;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.example.Main.props;
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
    private volatile String dumpA = props.getProperty("DEFAULT_DUMP_A");

    @Builder.Default
    private volatile String dumpB = props.getProperty("DEFAULT_DUMP_B");

    @Builder.Default
    private volatile String keysA = props.getProperty("DEFAULT_KEYS_A");

    @Builder.Default
    private volatile String keysB = props.getProperty("DEFAULT_KEYS_B");

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
        if (!s3link.matches(props.getProperty("INCOMPLETE_URL_REGEX"))) {
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
            this.s3linkA = new URL(props.getProperty("DEFAULT_S3_LINK"));
            this.s3linkB = new URL(props.getProperty("DEFAULT_S3_LINK"));
        } catch (MalformedURLException e) {
            log.error(props.getProperty("S3_LINK_NOT_INITIALIZED"), this.getRequestId());
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
            postTextResponseAsync(props.getProperty("BOT_SESSION_NOT_FOUND"), requestId);
            throw new IllegalStateException(
                props.getProperty("BOT_SESSION_DOES_NOT_EXIST") + requestId
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
        log.info(props.getProperty("REMOVING_BOT_SESSION"), requestId);
        allBotSessions.remove(requestId);
    }

    /**
     * initiate the downloading of the dump files.
     */
    public String initiateDownloading() throws InterruptedException {
        log.info(props.getProperty("DOWNLOAD_INITIATE"), this.getRequestId());
        this.setDownloadingStatus(DownloadingStatus.DOWNLOADING);
        Downloader downloader = this.getDownloader();
        downloader.addToDownloader(this.getS3linkA(), this.getDumpA());
        downloader.addToDownloader(this.getS3linkB(), this.getDumpB());

        long startTime = System.currentTimeMillis();
        try {
            boolean terminatedWithSuccess = this.getDownloader().download();
            if (!terminatedWithSuccess) {
                throw new InterruptedException(props.getProperty("DOWNLOAD_TIMEOUT"));
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            log.error(props.getProperty("DOWNLOAD_ERROR"), requestId);
            this.setDownloadingStatus(DownloadingStatus.NOT_DOWNLOADED);
            throw new InterruptedException(props.getProperty("DOWNLOAD_FAILED"));
        }
        long endTime = System.currentTimeMillis();
        log.info(props.getProperty("DOWNLOAD_SUCCESS"), endTime - startTime, requestId);
        this.setDownloadingTime(endTime - startTime);
        this.setDownloadingStatus(DownloadingStatus.DOWNLOADED); //volatile variable write
        return (
            props.getProperty("DOWNLOAD_COMPLETE_A") +
            (endTime - startTime) /
            1000.0 +
            props.getProperty("DOWNLOAD_COMPLETE_B")
        );
    }

    /**
     * initiate the parsing of the dump files.
     */
    public String initiateParsing() throws InterruptedException {
        log.info(props.getProperty("PARSE_INITIATE"), getRequestId());
        this.setParsingStatus(ParsingStatus.IN_PROGRESS);
        Parser parser = this.getParser();
        parser.addToParser(this.getDumpA(), this.getKeysA());
        parser.addToParser(this.getDumpB(), this.getKeysB());

        long startTime = System.currentTimeMillis();
        parser.parse();
        this.setParsingStatus(ParsingStatus.COMPLETED); //volatile variable write
        long endTime = System.currentTimeMillis();
        log.info(props.getProperty("PARSE_SUCCESS"), getRequestId(), endTime - startTime);
        this.setParsingTime(endTime - startTime);
        return (
            props.getProperty("PARSE_COMPLETE_A") +
            (endTime - startTime) /
            1000.0 +
            props.getProperty("PARSE_COMPLETE_B")
        );
    }

    /**
     * initiate the trie construction.
     */
    public String initiateTrieMaking() throws InterruptedException {
        this.setTrieMakingStatus(TrieMakingStatus.CONSTRUCTING);
        log.info(props.getProperty("MAKE_TRIE_INITIATE"), requestId);
        this.setTrieA(QTrie.builder().keysFile(this.getKeysA()).build());
        this.setTrieB(QTrie.builder().keysFile(this.getKeysB()).build());

        this.getTrieMaker().addToTrieMaker(this.getDumpA(), this.getTrieA());
        this.getTrieMaker().addToTrieMaker(this.getDumpB(), this.getTrieB());

        long startTime = System.currentTimeMillis();
        try {
            boolean terminatedWithSuccess = this.getTrieMaker().makeTries();
            if (!terminatedWithSuccess) {
                throw new Exception(props.getProperty("MAKE_TRIE_EXCEPTION"));
            }
        } catch (InterruptedException e) {
            this.setTrieMakingStatus(TrieMakingStatus.NOT_CONSTRUCTED);
            throw new InterruptedException(props.getProperty("MAKE_TRIE_INTERRUPTED") + requestId);
        } catch (Exception e) {
            this.setTrieMakingStatus(TrieMakingStatus.NOT_CONSTRUCTED);
            throw new InterruptedException(props.getProperty("MAKE_TRIE_TIMEOUT"));
        }
        long endTime = System.currentTimeMillis();
        log.info(props.getProperty("MAKE_TRIE_SUCCESS"), endTime - startTime, this.getRequestId());
        this.setTrieMakingTime(endTime - startTime);
        // log.info("time measures: {} {} {} {}", this.getRequestId(), this.getDownloadingTime() / 1000.0, this.getParsingTime() / 1000.0, this.getTrieMakingTime() / 1000.0);
        this.setTrieMakingStatus(TrieMakingStatus.CONSTRUCTED); //volatile variable write
        return (
            props.getProperty("MAKE_TRIE_COMPLETE_A") +
            (endTime - startTime) /
            1000.0 +
            props.getProperty("MAKE_TRIE_COMPLETE_B")
        );
    }
}
