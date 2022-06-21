package org.example;

import static org.example.BotSession.*;
import static org.messaging.PostUpdate.postTextResponseAsync;
import static org.messaging.PostUpdate.postTextResponseSync;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.querying.CountQuery;
import org.querying.NextKeyQuery;
import org.querying.Query;

/**
 * Utility class for the Slack Main class.
 * Provides methods for parsing, making tries and executing queries.
 */
@Slf4j
public class SlackUtils {

    private static final String DOWNLOADING_NOT_COMPLETED = "Downloading not completed.";
    private static final String DOWNLOADING_STARTED =
        "Downloading has started...\nPlease wait for automatic notification when downloading is done.\nOr use \"/download\" command again to check status.";
    private static final String DOWNLOADING_IN_PROGRESS = "Downloading in progress.\nPlease wait.";
    private static final String DOWNLOADING_COMPLETED = "Downloading completed";
    private static final String UNKNOWN_DOWNLOADING_BEHAVIOUR =
        "downloadUtils() is showing UNKNOWN behaviour: ";
    private static final String PARSING_NOT_COMPLETED =
        "Parsing not done. Please wait for parsing to finish or use \"/parse\" command to start parsing.";
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
    private static final String TRIE_CONSTRUCTION_NOT_COMPLETED = "Trie construction completed";
    private static final String UNKNOWN_TRIE_CONSTRUCTION_BEHAVIOUR =
        "parseUtils() is showing UNKNOWN behaviour: ";
    private static final String BAD_ARGUMENTS =
        "INVALID ARGUMENTS.\nRefer to \"/redis-bot-help\" for more information.";
    private static final String SESSION_IN_PROGRESS =
        "Could not create session. Consider re-trying command or running /clearall if the issue persists.\n";
    private static final String SESSION_CREATED =
        "A session has been created in this botSession. Ready to parse and make tries.\n";
    private static final String QUERYING_NOT_POSSIBLE =
        "Querying is not possible since tries have not been created.\n";
    private static final String QUERYING_POSSIBLE =
        "Querying is possible since tries have been created.\n";
    private static final String INVALID_REQUEST_ID =
        "Invalid Request Id. Use \"/list\" to see all active Request Ids.";
    private static final String ALL_PROCESSING_DONE =
        "Processing done. Files Downloaded, Parsed and Made into Tries.\nReady to answer queries.\n";

    /**
     * Create a botSession, download the files, parse the files and make tries.
     */
    public static String processAllUtils(final String text, final String channelId) {
        final String requestId = createBotSession();
        if (requestId == null) {
            return SESSION_IN_PROGRESS;
        }
        postTextResponseSync(
            SESSION_CREATED + "\n\n\n>Generated Request Id: " + requestId,
            channelId
        );

        final String downloadComplete = downloadUtils(requestId + " " + text, channelId, true);
        if (!downloadComplete.contains("Downloading completed")) {
            return downloadComplete + "\n\n\n" + DOWNLOADING_NOT_COMPLETED;
        }
        postTextResponseSync(downloadComplete, channelId);

        final String parsingComplete = parseUtils(requestId, channelId, true);
        if (!parsingComplete.contains("Parsing completed")) {
            return parsingComplete + "\n\n\n" + PARSING_NOT_COMPLETED;
        }
        postTextResponseSync(parsingComplete, channelId);

        final String trieComplete = makeTrieUtils(requestId, channelId, true);
        if (!trieComplete.contains("Trie construction completed")) {
            return trieComplete + "\n\n\n" + TRIE_CONSTRUCTION_NOT_COMPLETED;
        }
        postTextResponseSync(trieComplete, channelId);

        return ALL_PROCESSING_DONE + "\n\n\n>Your Request Id is: " + requestId;
    }

    /**
     * Create a botSession with the given channelId.
     *
     * @return SESSION_CREATED if a new session was created, SESSION_IN_PROGRESS if a session was already open.
     */
    public static String createSessionUtils() {
        String requestId = createBotSession();
        if (requestId != null) {
            return SESSION_CREATED + "\n\n\n>Generated Request Id: " + requestId;
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
    public static String downloadUtils(
        final String text,
        final String channelId,
        boolean waitForCompletion
    ) {
        BotSession botSession;
        try {
            assert (!text.isEmpty());
            String[] args = text.split("\\r?\\n|\\r|\\s");
            assert (args.length == 3);
            botSession = getBotSession(args[0]);
            botSession.setS3linkA(new URL(BotSession.elongateURL(args[1])));
            botSession.setS3linkB(new URL(BotSession.elongateURL(args[2])));
            log.info(
                "Downloading files from S3 links: " +
                botSession.getS3linkA() +
                " and " +
                botSession.getS3linkB()
            );
        } catch (IllegalStateException e) {
            return INVALID_REQUEST_ID;
        } catch (Exception e) {
            return BAD_ARGUMENTS;
        }

        String requestId = botSession.getRequestId();
        class DownloadCallable implements Callable<String> {

            @Override
            public String call() throws Exception {
                return botSession.initiateDownloading();
            }
        }

        class DownloadRunnable implements Runnable {

            @Override
            public void run() {
                try {
                    String response = botSession.initiateDownloading();
                    postTextResponseAsync(response, channelId);
                } catch (Exception e) {
                    log.error("Error in downloadRunnable", e);
                    String response = "Error in downloadRunnable: " + e.getMessage();
                    postTextResponseAsync(response, channelId);
                }
            }
        }

        if (botSession.getExecutedDownloading().compareAndSet(false, true)) {
            log.info("downloading started for botSession {}", requestId);
            if (waitForCompletion) {
                Future<String> downloadCallable = botSession
                    .getDownloadingExecutorService()
                    .submit(new DownloadCallable());
                try {
                    String response = downloadCallable.get();
                    botSession.getDownloadingExecutorService().shutdown();
                    return response;
                } catch (InterruptedException e) {
                    log.error("InterruptedError in downloadCallable", e);
                    return "InterruptedError in downloadCallable: " + e.getMessage();
                } catch (ExecutionException e) {
                    log.error("ExecutionException in downloadCallable", e);
                    return "ExecutionException in downloadCallable: " + e.getMessage();
                }
            } else {
                botSession.getDownloadingExecutorService().submit(new DownloadRunnable());
                botSession.getDownloadingExecutorService().shutdown();
                return DOWNLOADING_STARTED;
            }
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
    public static String parseUtils(
        final String requestId,
        final String channelId,
        boolean waitForCompletion
    ) {
        try {
            BotSession botSession = getBotSession(requestId);
            if (!botSession.getDownloadingStatus().equals(DownloadingStatus.DOWNLOADED)) {
                return DOWNLOADING_NOT_COMPLETED;
            }

            class ParserCallable implements Callable<String> {

                @Override
                public String call() throws Exception {
                    return botSession.initiateParsing();
                }
            }

            class ParserRunnable implements Runnable {

                @Override
                public void run() {
                    try {
                        String response = botSession.initiateParsing();
                        postTextResponseAsync(response, channelId);
                    } catch (Exception e) {
                        log.error("Error in parserRunnable", e);
                        String response = "Error in parserRunnable: " + e.getMessage();
                        postTextResponseAsync(response, channelId);
                    }
                }
            }

            if (botSession.getExecutedParsing().compareAndSet(false, true)) {
                if (waitForCompletion) {
                    Future<String> parserCallable = botSession
                        .getParsingExecutorService()
                        .submit(new ParserCallable());
                    try {
                        String response = parserCallable.get();
                        botSession.getParsingExecutorService().shutdown();
                        return response;
                    } catch (InterruptedException e) {
                        log.error("InterruptedError in parserCallable", e);
                        return "InterruptedError in parserCallable: " + e.getMessage();
                    } catch (ExecutionException e) {
                        log.error("ExecutionException in parserCallable", e);
                        return "ExecutionException in parserCallable: " + e.getMessage();
                    }
                } else {
                    botSession.getParsingExecutorService().submit(new ParserRunnable());
                    botSession.getParsingExecutorService().shutdown();
                    return PARSING_STARTED;
                }
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
    public static String makeTrieUtils(
        final String requestId,
        final String channelId,
        boolean waitForCompletion
    ) {
        try {
            BotSession botSession = getBotSession(requestId);
            if (!botSession.getParsingStatus().equals(ParsingStatus.COMPLETED)) {
                return PARSING_NOT_COMPLETED;
            }

            class TrieMakerCallable implements Callable<String> {

                @Override
                public String call() throws Exception {
                    return botSession.initiateTrieMaking();
                }
            }

            class TrieMakerRunnable implements Runnable {

                @Override
                public void run() {
                    try {
                        String response = botSession.initiateTrieMaking();
                        postTextResponseAsync(response, channelId);
                    } catch (Exception e) {
                        log.error("Error in trieMakerRunnable", e);
                        String response = "Error in trieMakerRunnable: " + e.getMessage();
                        postTextResponseAsync(response, channelId);
                    }
                }
            }

            if (botSession.getExecutedTrieMaking().compareAndSet(false, true)) {
                if (waitForCompletion) {
                    Future<String> trieMakerCallable = botSession
                        .getTrieMakingExecutorService()
                        .submit(new TrieMakerCallable());
                    try {
                        String response = trieMakerCallable.get();
                        botSession.getTrieMakingExecutorService().shutdown();
                        return response;
                    } catch (InterruptedException e) {
                        log.error("InterruptedError in trieMakerCallable", e);
                        return "InterruptedError in trieMakerCallable: " + e.getMessage();
                    } catch (ExecutionException e) {
                        log.error("ExecutionException in trieMakerCallable", e);
                        return "ExecutionException in trieMakerCallable: " + e.getMessage();
                    }
                } else {
                    botSession.getTrieMakingExecutorService().submit(new TrieMakerRunnable());
                    botSession.getTrieMakingExecutorService().shutdown();
                    return DOWNLOADING_STARTED;
                }
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
     * @param text: the query arguments
     * @return String containing the query result or error message
     */
    public static String countUtils(final String text) {
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
        try {
            BotSession botSession = getBotSession(requestId);
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
     * @param text: [requestId] [prefixKey] [count]
     * @return String containing the query result or error message
     */
    public static String getNextKeyUtils(String text) {
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

        try {
            BotSession botSession = getBotSession(requestId);
            if (!botSession.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTED)) {
                return TRIES_NOT_CREATED;
            }
            Query query = NextKeyQuery
                .builder()
                .key(prefixKey)
                .n(count)
                .queryType(NextKeyQuery.QueryType.GET_NEXT)
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
        BotSession.getAllBotSessions().forEach((key, value) -> deleteSessionUtils(key));
        return "Deleted: all sessions.";
    }

    /**
     * List all active sessions
     *
     * @return String containing the list of active sessions with requestId and the s3links
     */
    public static String listSessionsUtils() {
        StringBuilder sb = new StringBuilder();
        if (BotSession.getAllBotSessions().isEmpty()) {
            sb.append(">No sessions are active.");
        } else {
            sb.append("Active sessions: \n\n");
            final int[] index = { 1 };
            BotSession
                .getAllBotSessions()
                .forEach((key, channel) -> {
                    sb
                        .append(index[0])
                        .append(". Request Id: `")
                        .append(channel.getRequestId())
                        .append("`:\n>A: <")
                        .append(channel.getS3linkA())
                        .append("|")
                        .append(channel.getS3linkA())
                        .append(">\n>B: <")
                        .append(channel.getS3linkB())
                        .append("|")
                        .append(channel.getS3linkB())
                        .append(">\n\n");
                    index[0]++;
                });
        }
        return sb.toString();
    }

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
