package org.example;

import static org.example.BotSession.*;
import static org.messaging.PostUpdate.postTextResponseAsync;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.processing.Downloader;
import org.processing.Parser;
import org.querying.CountQuery;
import org.querying.NextKeyQuery;
import org.querying.Query;
import org.trie.QTrie;

/**
 * Utility class for the Slack Main class.
 * Provides methods for parsing, making tries and executing queries.
 */
@Slf4j
public class SlackUtils {

    private static final String DOWNLOADING_NOT_COMPLETED =
        "Downloading not completed.\nPlease wait for downloading to finish or use \"/download\" command to start parsing..";
    private static final String DOWNLOADING_STARTED =
        "Downloading has started...\nPlease wait for automatic notification when downloading is done.\nOr use \"/download\" command again to check status.";
    private static final String DOWNLOADING_IN_PROGRESS = "Downloading in progress.\nPlease wait.";
    private static final String DOWNLOADING_COMPLETED = "Downloading completed";
    private static final String PARSING_NOT_COMPLETED =
        "Parsing not done. Please wait for parsing to finish or use \"/parse\" command to start parsing.";
    private static final String UNKNOWN_DOWNLOADING_BEHAVIOUR =
        "downloadUtils() is showing UNKNOWN behaviour: ";
    private static final String PARSING_STARTED =
        "Parsing has started...\nPlease wait for automatic notification when parsing is done.\nOr use \"/parse\" command again to check status.";
    private static final String PARSING_IN_PROGRESS = "Parsing in progress.\nPlease wait.";
    private static final String PARSING_COMPLETED = "Parsing completed";
    private static final String UNKNOWN_PARSING_BEHAVIOUR =
        "parseUtils() is showing UNKNOWN behaviour: ";
    private static final String TRIES_NOT_CREATED =
        "Tries not created.\nPlease wait for tries to be created\nOr use \"/maketrie\" command to start creating tries.";
    private static final String TRIE_CONSTRUCTION_STARTED =
        "Trie construction started...\nPlease wait for automatic notification when construction is over.\nOr use \"/maketrie\" command again to check status.";
    private static final String TRIE_CONSTRUCTION_IN_PROGRESS =
        "Trie construction in progress.\nPlease wait.";
    private static final String TRIE_CONSTRUCTION_COMPLETED = "Trie construction completed";
    private static final String UNKNOWN_TRIE_CONSTRUCTION_BEHAVIOUR =
        "parseUtils() is showing UNKNOWN behaviour: ";
    private static final String BAD_ARGUMENTS =
        "INVALID ARGUMENTS.\nRefer to \"/redis-bot-help\" for more information.";
    private static final String SESSION_IN_PROGRESS =
        "A session is already open in this botSession.\n";
    private static final String SESSION_CREATED =
        "A session has been created in this botSession. Ready to parse and make tries.\n";
    private static final String QUERYING_NOT_POSSIBLE =
        "Querying is not possible since tries have not been created.\n";
    private static final String QUERYING_POSSIBLE =
        "Querying is possible since tries have been created.\n";
    private static final String INVALID_REQUEST_ID =
        "Invalid Request Id. Use \"/list\" to see all active Request Ids.";

    /**
     * Create a botSession with the given channelId.
     *
     * @return SESSION_CREATED if a new session was created, SESSION_IN_PROGRESS if a session was already open.
     */
    public static String createSessionUtils() {
        String requestId = createBotSession();
        if (requestId != null) {
            return SESSION_CREATED + "\n\n\n>Your response Id: " + requestId + "";
        } else {
            return SESSION_IN_PROGRESS;
        }
    }

    /**
     * Checks if the downloading is in progress or not and if not started, execute downloading in parallel
     * This method is only invoked by the command "/download". For interactive downloading, there is no method yet.
     *
     * @param text:     [requestId] [s3linkA] [s3linkB], space-separated requestId of the session and s3links
     * @param channelId : channelId of the request
     * @return downloading status of the current botSession attached to the requestId
     */
    public static String downloadUtils(final String text, final String channelId) {
        URL[] urls;
        BotSession botSession;
        try {
            assert (!text.isEmpty());
            String[] args = StringUtils.split(text);
            assert (args.length == 3);
            botSession = getBotSession(args[0]);
            botSession.setS3linkA(new URL(args[1]));
            botSession.setS3linkB(new URL(args[2]));
        } catch (IllegalStateException e) {
            return INVALID_REQUEST_ID;
        } catch (Exception e) {
            return BAD_ARGUMENTS;
        }

        String requestId = botSession.getRequestId();

        if (botSession.getExecutedDownloading().compareAndSet(false, true)) {
            log.info("downloading started for botSession {}", requestId);
            botSession
                .getDownloadingExecutorService()
                .submit(() -> {
                    botSession.setDownloadingStatus(DownloadingStatus.DOWNLOADING);
                    Downloader downloader = botSession.getDownloader();
                    downloader.addToDownloader(botSession.getS3linkA(), botSession.getDumpA());
                    downloader.addToDownloader(botSession.getS3linkB(), botSession.getDumpB());

                    long startTime = System.currentTimeMillis();
                    try {
                        boolean terminatedWithSuccess = botSession.getDownloader().download();
                        if (!terminatedWithSuccess) {
                            throw new InterruptedException("Timeout Exception while downloading");
                        }
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                        log.error(
                            "download interrupted due to FileNotFound, SecurityException or DownloadingError for botSession {}",
                            requestId
                        );
                        botSession.setDownloadingStatus(DownloadingStatus.NOT_DOWNLOADED);
                        postTextResponseAsync(
                            "\uD83D\uDEA8\uD83D\uDEA8 Download failed \uD83D\uDEA8\uD83D\uDEA8",
                            requestId
                        );
                        throw new RuntimeException(e);
                    }
                    long endTime = System.currentTimeMillis();
                    log.info(
                        "Download completed in {} milliseconds in botSession {}",
                        endTime - startTime,
                        requestId
                    );
                    botSession.setDownloadingTime(endTime - startTime);
                    botSession.setDownloadingStatus(DownloadingStatus.DOWNLOADED); //volatile variable write
                    postTextResponseAsync(
                        "\uD83D\uDEA8\uD83D\uDEA8 Download completed in " +
                        (endTime - startTime) /
                        1000.0 +
                        " second(s). \uD83D\uDEA8\uD83D\uDEA8",
                        channelId
                    );
                });
            botSession.getDownloadingExecutorService().shutdown();
            return DOWNLOADING_STARTED;
        } else {
            if (botSession.getDownloadingStatus().equals(DownloadingStatus.DOWNLOADING)) {
                return DOWNLOADING_IN_PROGRESS;
            } else if (botSession.getDownloadingStatus().equals(DownloadingStatus.DOWNLOADED)) {
                return (
                    DOWNLOADING_COMPLETED +
                    " in " +
                    botSession.getDownloadingTime() /
                    1000.0 +
                    " second(s)."
                );
            } else {
                return UNKNOWN_DOWNLOADING_BEHAVIOUR + botSession.getDownloadingStatus();
            }
        }
    }

    /**
     * Checks if the parsing is in progress or not and if not started, execute parsing in parallel
     * This method is only invoked by the command "/parse". For interactive parsing, use ParseAndMakeTrieView class
     *
     * @param requestId: requestId of the session
     * @param channelId  : channelId of the request
     */
    public static String parseUtils(final String requestId, final String channelId) {
        try (BotSession botSession = getBotSession(requestId)) {
            if (!botSession.getDownloadingStatus().equals(DownloadingStatus.DOWNLOADED)) {
                return DOWNLOADING_NOT_COMPLETED;
            }

            if (botSession.getExecutedParsing().compareAndSet(false, true)) {
                log.info("parsing started for botSession {}", requestId);
                botSession
                    .getParsingExecutorService()
                    .submit(() -> {
                        botSession.setParsingStatus(ParsingStatus.IN_PROGRESS);
                        Parser parser = botSession.getParser();
                        parser.addToParser(botSession.getDumpA(), botSession.getKeysA());
                        parser.addToParser(botSession.getDumpB(), botSession.getKeysB());

                        long startTime = System.currentTimeMillis();
                        parser.parse();
                        botSession.setParsingStatus(ParsingStatus.COMPLETED); //volatile variable write
                        long endTime = System.currentTimeMillis();

                        log.info(
                            "parsing completed for botSession {} in {} ms",
                            requestId,
                            endTime - startTime
                        );
                        botSession.setParsingTime(endTime - startTime);
                        postTextResponseAsync(
                            "\uD83D\uDEA8\uD83D\uDEA8 Parsing completed in " +
                            (endTime - startTime) /
                            1000.0 +
                            " second(s). \uD83D\uDEA8\uD83D\uDEA8",
                            channelId
                        );
                    });
                botSession.getParsingExecutorService().shutdown();
                return PARSING_STARTED;
            } else {
                if (botSession.getParsingStatus().equals(ParsingStatus.IN_PROGRESS)) {
                    return PARSING_IN_PROGRESS;
                } else if (botSession.getParsingStatus().equals(ParsingStatus.COMPLETED)) {
                    return (
                        PARSING_COMPLETED +
                        " in " +
                        botSession.getParsingTime() /
                        1000.0 +
                        " second(s)."
                    );
                } else {
                    return UNKNOWN_PARSING_BEHAVIOUR + botSession.getParsingStatus();
                }
            }
        } catch (IllegalStateException e) {
            log.error(e.getMessage());
            return INVALID_REQUEST_ID;
        }
    }

    /**
     * Checks if the tries are in progress or not and if not started, execute tries in parallel
     * This method is only invoked by the command "/maketrie". For interactive tries, use ParseAndMakeTrieView class
     *
     * @param requestId: requestId of the session
     * @param channelId  : channelId to post the reply
     * @return trie making status of the session (started, in progress, completed)
     */
    public static String trieConstructionUtils(final String requestId, final String channelId) {
        try (BotSession botSession = getBotSession(requestId)) {
            if (!botSession.getParsingStatus().equals(ParsingStatus.COMPLETED)) {
                return PARSING_NOT_COMPLETED;
            }

            if (botSession.getExecutedTrieMaking().compareAndSet(false, true)) {
                botSession.setTrieMakingStatus(TrieMakingStatus.CONSTRUCTING);
                log.info("trie construction started for botSession {}", requestId);
                botSession
                    .getTrieMakingExecutorService()
                    .submit(() -> {
                        botSession.setTrieA(
                            QTrie.builder().keysFile(botSession.getKeysA()).build()
                        );
                        botSession.setTrieB(
                            QTrie.builder().keysFile(botSession.getKeysB()).build()
                        );

                        botSession
                            .getTrieMaker()
                            .addToTrieMaker(botSession.getDumpA(), botSession.getTrieA());
                        botSession
                            .getTrieMaker()
                            .addToTrieMaker(botSession.getDumpB(), botSession.getTrieB());

                        long startTime = System.currentTimeMillis();
                        try {
                            boolean terminatedWithSuccess = botSession.getTrieMaker().makeTries();
                            if (!terminatedWithSuccess) {
                                throw new Exception("Timeout Exception while making tries");
                            }
                        } catch (InterruptedException e) {
                            log.error(
                                "trie construction interrupted due trie-initializer-threads being interrupted for botSession {}",
                                channelId
                            );
                            botSession.setTrieMakingStatus(TrieMakingStatus.NOT_CONSTRUCTED);
                            postTextResponseAsync(
                                "\uD83D\uDEA8\uD83D\uDEA8 Trie construction failed \uD83D\uDEA8\uD83D\uDEA8",
                                channelId
                            );
                            throw new RuntimeException(e);
                        } catch (Exception e) {
                            log.error(
                                "trie construction interrupted due to timeout for botSession {}",
                                channelId
                            );
                            botSession.setTrieMakingStatus(TrieMakingStatus.NOT_CONSTRUCTED);
                            postTextResponseAsync(
                                "\uD83D\uDEA8\uD83D\uDEA8 Trie construction failed \uD83D\uDEA8\uD83D\uDEA8",
                                channelId
                            );
                            throw new RuntimeException(e);
                        }
                        long endTime = System.currentTimeMillis();
                        log.info(
                            "Trie construction completed in {} milliseconds in botSession {}",
                            endTime - startTime,
                            channelId
                        );
                        botSession.setTrieMakingTime(endTime - startTime);
                        botSession.setTrieMakingStatus(TrieMakingStatus.CONSTRUCTED); //volatile variable write
                        postTextResponseAsync(
                            "\uD83D\uDEA8\uD83D\uDEA8 Trie construction completed in " +
                            (endTime - startTime) /
                            1000.0 +
                            " second(s). \uD83D\uDEA8\uD83D\uDEA8",
                            channelId
                        );
                    });
                botSession.getTrieMakingExecutorService().shutdown();
                return TRIE_CONSTRUCTION_STARTED;
            } else {
                if (botSession.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTING)) {
                    return TRIE_CONSTRUCTION_IN_PROGRESS;
                } else if (botSession.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTED)) {
                    return (
                        TRIE_CONSTRUCTION_COMPLETED +
                        " in " +
                        botSession.getTrieMakingTime() /
                        1000.0 +
                        " second(s)."
                    );
                } else {
                    return UNKNOWN_TRIE_CONSTRUCTION_BEHAVIOUR + botSession.getTrieMakingStatus();
                }
            }
        } catch (IllegalStateException e) {
            log.error(e.getMessage());
            return INVALID_REQUEST_ID;
        }
    }

    /**
     * - Check if the "/getcount" query can execute or not
     * - Check if the query arguments are valid or not
     *
     * @param text:      the query arguments
     * @param channelId: the botSession to check the status of
     * @return String containing the query result or error message
     */
    public static String countUtils(final String text, final String channelId) {
        String requestId;
        String prefixKey;
        try {
            assert !text.isEmpty();
            String[] queryArgs = text.split(" ");
            assert queryArgs.length == 2;
            requestId = queryArgs[0];
            prefixKey = queryArgs[1];
        } catch (Exception e) {
            log.error(e.getMessage());
            return BAD_ARGUMENTS;
        }
        try (BotSession botSession = getBotSession(requestId)) {
            if (!botSession.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTED)) {
                return TRIES_NOT_CREATED;
            }
            log.info("Counting for key: " + prefixKey);
            Query query = CountQuery
                .builder()
                .key(prefixKey)
                .queryType(Query.QueryType.GET_COUNT)
                .requestId(requestId)
                .build();
            query.execute();
            return query.result();
        } catch (IllegalStateException e) {
            log.error(e.getMessage());
            return INVALID_REQUEST_ID;
        }
    }

    /**
     * - Check if the "/getcount" query can execute or not
     * - Check if the query arguments are valid or not
     *
     * @param text:      [requestId] [prefixKey] [count]
     * @param channelId: the botSession to check the status of
     * @return String containing the query result or error message
     */
    public static String getNextKeyUtils(String text, final String channelId) {
        String requestId;
        String prefixKey;
        int count;
        try {
            assert (!text.isEmpty());
            String[] tokens = StringUtils.split(text);
            assert (tokens.length == 3);
            requestId = tokens[0];
            prefixKey = tokens[1];
            count = Integer.parseInt(tokens[2]);
            log.info(
                "Getting next key for key: " +
                prefixKey +
                " count: " +
                count +
                " requestId: " +
                requestId
            );
            assert count > 0;
        } catch (Exception e) {
            log.error(e.getMessage());
            return BAD_ARGUMENTS;
        }

        try (BotSession botSession = getBotSession(requestId)) {
            if (!botSession.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTED)) {
                return TRIES_NOT_CREATED;
            }
            Query query = NextKeyQuery
                .builder()
                .key(prefixKey)
                .n(count)
                .queryType(NextKeyQuery.QueryType.TOP_K_CHILDREN)
                .requestId(requestId)
                .build();
            query.execute();
            return query.result();
        } catch (IllegalStateException e) {
            log.error(e.getMessage());
            return INVALID_REQUEST_ID;
        }
    }

    /**
     * Clears the given botSession's data by:
     * - removing the botSession from the list of channels
     * - deleting the botSession's dump file and keys files (TODO: InputRDB)
     *
     * @param requestId: the botSession to clear the session in
     * @return String containing the delete-success message to be sent to the botSession
     */
    public static String deleteSessionUtils(final String requestId) {
        BotSession botSession;
        try {
            botSession = getBotSession(requestId);
        } catch (Exception e) {
            log.error("Error in deleting session for botSession {}", requestId);
            return BAD_ARGUMENTS;
        }
        try {
            log.info("Deleting botSession: " + requestId);
            if (Files.exists(Paths.get(botSession.getDumpA()))) {
                Files.delete(Paths.get(botSession.getDumpA()));
                log.info("Deleted dump file: " + botSession.getDumpA());
            } else {
                log.info("Cannot delete dump file. Does not exist: " + botSession.getDumpA());
            }

            if (Files.exists(Paths.get(botSession.getDumpB()))) {
                Files.delete(Paths.get(botSession.getDumpB()));
                log.info("Deleted dump file: " + botSession.getDumpB());
            } else {
                log.info("Cannot delete dump file. Does not exist: " + botSession.getDumpB());
            }

            if (Files.exists(Paths.get(botSession.getKeysA()))) {
                Files.delete(Paths.get(botSession.getKeysA()));
                log.info("Deleted keys file: " + botSession.getKeysA());
            } else {
                log.info("Cannot delete keys file. Does not exist: " + botSession.getKeysA());
            }

            if (Files.exists(Paths.get(botSession.getKeysB()))) {
                Files.delete(Paths.get(botSession.getKeysB()));
                log.info("Deleted keys file: " + botSession.getKeysB());
            } else {
                log.info("Cannot delete keys file. Does not exist: " + botSession.getKeysB());
            }
        } catch (IOException e) {
            log.error("Error deleting files for botSession {}", requestId);
            throw new RuntimeException(e);
        }
        removeBotSession(requestId);
        return "Deleted: session for Response Id: " + requestId;
    }

    /**
     * Deletes all active sessions
     *
     * @return String containing the delete-success message to be sent to the channels
     */
    public static String deleteAllSessionsUtils() {
        BotSession
            .getBotSessions()
            .forEach((key, value) -> {
                deleteSessionUtils(key);
            });
        return "Deleted: all sessions.";
    }

    /**
     * List all active sessions
     *
     * @return String containing the list of active sessions with requestId and the s3links
     */
    public static String listSessionsUtils() {
        StringBuilder sb = new StringBuilder();
        if (BotSession.getBotSessions().isEmpty()) {
            sb.append(">No sessions are active.");
        } else {
            sb.append("Active sessions: \n\n");
            final int[] index = { 1 };
            BotSession
                .getBotSessions()
                .forEach((key, channel) -> {
                    sb
                        .append(index[0])
                        .append(". Request Id: `")
                        .append(channel.getRequestId())
                        .append("`:\n>A: <")
                        .append(channel.getS3linkA().toString())
                        .append("|")
                        .append(BotSession.formatLink(channel.getS3linkA().toString()))
                        .append(">\n>B: <")
                        .append(channel.getS3linkB().toString())
                        .append("|")
                        .append(BotSession.formatLink(channel.getS3linkB().toString()))
                        .append(">\n\n");
                    index[0]++;
                });
        }
        return sb.toString();
    }

    // not fixed below

    /**
     * Checks if executing queries is possible or not
     *
     * @param channelId: the botSession to start the session
     * @return QUERYING_NOT_POSSIBLE if queries cannot be executed, else returns empty string
     */
    public static String queryAllUtils(final String channelId) {
        BotSession botSession = getBotSession(channelId);
        if (!botSession.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTED)) {
            return QUERYING_NOT_POSSIBLE;
        }
        return QUERYING_POSSIBLE;
    }

    /**
     * Resets the internal parameters of the botSession(or 'session') to their default values.
     * Cheats by deleting the botSession and creating a new session.
     *
     * @param channelId: the botSession to reset the session in
     */
    public static String resetSessionUtils(final String channelId) {
        BotSession botSession = getBotSession(channelId);
        deleteSessionUtils(channelId);
        String newRequestId = createSessionUtils();
        return (
            "BotSession has been reset to default values. Tries and input files deleted. Session state variables reset.\n\n\n>New response Id: " +
            newRequestId
        );
    }
}
