package org.example;

import static org.example.BotSession.*;
import static org.example.Main.props;
import static org.messaging.PostUpdate.postTextResponseAsync;
import static org.messaging.PostUpdate.postTextResponseSync;

import java.io.*;
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

    /**
     * Create a botSession, download the files, parse the files and make tries.
     */
    public static String processAllUtils(final String text, final String channelId) {
        final String requestId = createBotSession();
        if (requestId == null) {
            return props.getProperty("SESSION_IN_PROGRESS");
        }
        postTextResponseSync(props.getProperty("SESSION_CREATED") + requestId, channelId);

        final String downloadComplete = downloadUtils(requestId + " " + text, channelId, true);
        if (!downloadComplete.contains(props.getProperty("DOWNLOADING_COMPLETED"))) {
            return downloadComplete + "\n\n\n" + props.getProperty("DOWNLOADING_NOT_COMPLETED");
        }
        postTextResponseSync(downloadComplete, channelId);

        final String parsingComplete = parseUtils(requestId, channelId, true);
        if (!parsingComplete.contains(props.getProperty("PARSING_COMPLETED"))) {
            return parsingComplete + "\n\n\n" + props.getProperty("PARSING_NOT_COMPLETED");
        }
        postTextResponseSync(parsingComplete, channelId);

        final String trieComplete = makeTrieUtils(requestId, channelId, true);
        if (!trieComplete.contains(props.getProperty("TRIE_CONSTRUCTION_COMPLETED"))) {
            return trieComplete + "\n\n\n" + props.getProperty("TRIE_CONSTRUCTION_NOT_COMPLETED");
        }
        postTextResponseSync(trieComplete, channelId);

        return props.getProperty("ALL_PROCESSING_DONE") + "\n\n\n>Your Request Id is: " + requestId;
    }

    /**
     * Create a botSession with the given channelId.
     *
     * @return SESSION_CREATED if a new session was created, SESSION_IN_PROGRESS if a session was already open.
     */
    public static String createSessionUtils() {
        String requestId = createBotSession();
        if (requestId != null) {
            return (
                    props.getProperty("SESSION_CREATED") + "\n\n\n>Generated Request Id: " + requestId
            );
        } else {
            return props.getProperty("SESSION_IN_PROGRESS");
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
            return props.getProperty("INVALID_REQUEST_ID");
        } catch (Exception e) {
            return props.getProperty("BAD_ARGUMENTS");
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
                    log.error(props.getProperty("DOWNLOADING_RUNNABLE_ERROR") + e);
                    String response =
                            props.getProperty("DOWNLOADING_RUNNABLE_ERROR") + e.getMessage();
                    postTextResponseAsync(response, channelId);
                }
            }
        }

        if (botSession.getExecutedDownloading().compareAndSet(false, true)) {
            log.info(props.getProperty("DOWNLOADING_INITIATED"), requestId);
            if (waitForCompletion) {
                Future<String> downloadCallable = botSession
                        .getDownloadingExecutorService()
                        .submit(new DownloadCallable());
                try {
                    String response = downloadCallable.get();
                    botSession.getDownloadingExecutorService().shutdown();
                    return response;
                } catch (InterruptedException e) {
                    log.error(props.getProperty("DOWNLOADING_CALLABLE_ERROR_INTERRUPT") + e);
                    return (
                            props.getProperty("DOWNLOADING_CALLABLE_ERROR_INTERRUPT") + e.getMessage()
                    );
                } catch (ExecutionException e) {
                    log.error(props.getProperty("DOWNLOADING_CALLABLE_ERROR_EXEC") + e);
                    return props.getProperty("DOWNLOADING_CALLABLE_ERROR_EXEC") + e.getMessage();
                }
            } else {
                botSession.getDownloadingExecutorService().submit(new DownloadRunnable());
                botSession.getDownloadingExecutorService().shutdown();
                return props.getProperty("DOWNLOADING_STARTED");
            }
        } else {
            if (botSession.getDownloadingStatus().equals(DownloadingStatus.DOWNLOADING)) {
                return props.getProperty("DOWNLOADING_IN_PROGRESS");
            } else if (botSession.getDownloadingStatus().equals(DownloadingStatus.DOWNLOADED)) {
                return (
                        props.getProperty("DOWNLOADING_COMPLETED") +
                                " in " +
                                botSession.getDownloadingTime() /
                                        1000.0 +
                                " second(s)."
                );
            } else {
                return (
                        props.getProperty("UNKNOWN_DOWNLOADING_BEHAVIOUR") +
                                botSession.getDownloadingStatus()
                );
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
                return props.getProperty("DOWNLOADING_NOT_COMPLETED");
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
                        log.error(props.getProperty("PARSING_RUNNABLE_ERROR") + e);
                        String response =
                                props.getProperty("PARSING_RUNNABLE_ERROR") + e.getMessage();
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
                        log.error(props.getProperty("PARSING_CALLABLE_ERROR_INTERRUPT") + e);
                        return (
                                props.getProperty("PARSING_CALLABLE_ERROR_INTERRUPT") + e.getMessage()
                        );
                    } catch (ExecutionException e) {
                        log.error(props.getProperty("PARSING_CALLABLE_ERROR_EXEC") + e);
                        return props.getProperty("PARSING_CALLABLE_ERROR_EXEC") + e.getMessage();
                    }
                } else {
                    botSession.getParsingExecutorService().submit(new ParserRunnable());
                    botSession.getParsingExecutorService().shutdown();
                    return props.getProperty("PARSING_STARTED");
                }
            } else {
                if (botSession.getParsingStatus().equals(ParsingStatus.IN_PROGRESS)) {
                    return props.getProperty("PARSING_IN_PROGRESS");
                } else if (botSession.getParsingStatus().equals(ParsingStatus.COMPLETED)) {
                    return (
                            props.getProperty("PARSING_COMPLETED") +
                                    " in " +
                                    botSession.getParsingTime() /
                                            1000.0 +
                                    " second(s)."
                    );
                } else {
                    return (
                            props.getProperty("UNKNOWN_PARSING_BEHAVIOUR") +
                                    botSession.getParsingStatus()
                    );
                }
            }
        } catch (IllegalStateException e) {
            log.error(e.getMessage());
            return props.getProperty("INVALID_REQUEST_ID");
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
                return props.getProperty("PARSING_NOT_COMPLETED");
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
                        log.error(props.getProperty("TRIE_CONSTRUCTION_RUNNABLE_ERROR") + e);
                        String response =
                                props.getProperty("TRIE_CONSTRUCTION_RUNNABLE_ERROR") + e.getMessage();
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
                        log.error(
                                props.getProperty("TRIE_CONSTRUCTION_CALLABLE_ERROR_INTERRUPT") + e
                        );
                        return (
                                props.getProperty("TRIE_CONSTRUCTION_CALLABLE_ERROR_INTERRUPT") +
                                        e.getMessage()
                        );
                    } catch (ExecutionException e) {
                        log.error(props.getProperty("TRIE_CONSTRUCTION_CALLABLE_ERROR_EXEC") + e);
                        return (
                                props.getProperty("TRIE_CONSTRUCTION_CALLABLE_ERROR_EXEC") +
                                        e.getMessage()
                        );
                    }
                } else {
                    botSession.getTrieMakingExecutorService().submit(new TrieMakerRunnable());
                    botSession.getTrieMakingExecutorService().shutdown();
                    return props.getProperty("TRIE_CONSTRUCTION_STARTED");
                }
            } else {
                if (botSession.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTING)) {
                    return props.getProperty("TRIE_CONSTRUCTION_IN_PROGRESS");
                } else if (botSession.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTED)) {
                    return (
                            props.getProperty("TRIE_CONSTRUCTION_COMPLETED") +
                                    " in " +
                                    botSession.getTrieMakingTime() /
                                            1000.0 +
                                    " second(s)."
                    );
                } else {
                    return (
                            props.getProperty("UNKNOWN_TRIE_CONSTRUCTION_BEHAVIOUR") +
                                    botSession.getTrieMakingStatus()
                    );
                }
            }
        } catch (IllegalStateException e) {
            log.error(e.getMessage());
            return props.getProperty("INVALID_REQUEST_ID");
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
        Integer head;
        log.info("countutils: " + text);
        try {
            assert !text.isEmpty();
            String[] queryArgs = text.split(" ");
            assert queryArgs.length == 2 || queryArgs.length == 3;
            requestId = queryArgs[0];
            prefixKey = queryArgs[1];
            if (queryArgs.length == 3) {
                head = Integer.parseInt(queryArgs[2]);
            } else {
                head = Integer.parseInt(props.getProperty("DEFAULT_HEAD"));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return props.getProperty("BAD_ARGUMENTS");
        }
        try {
            BotSession botSession = getBotSession(requestId);
            assert botSession != null;
            if (!botSession.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTED)) {
                return props.getProperty("TRIES_NOT_CREATED");
            }
            log.info(props.getProperty("GETCOUNT_QUERY"), prefixKey);
            Query query = CountQuery
                    .builder()
                    .key(prefixKey)
                    .queryType(Query.QueryType.GET_COUNT)
                    .head(head)
                    .requestId(requestId)
                    .build();
            query.execute();
            return query.result();
        } catch (IllegalStateException e) {
            log.error(e.getMessage());
            return props.getProperty("INVALID_REQUEST_ID");
        }
    }

    public static String getDiffUtils(String text) {
        String requestId = text;
        BotSession botSession = getBotSession(requestId);
        String fileA = botSession.getKeysA() + "sorted.txt";
        String fileB = botSession.getKeysB() + "sorted.txt";

        try (
                FileReader fileReaderA = new FileReader(fileA);
                BufferedReader readerA = new BufferedReader(fileReaderA);
                FileReader fileReaderB = new FileReader(fileB);
                BufferedReader readerB = new BufferedReader(fileReaderB);
                FileWriter fileWriterA = new FileWriter("./.sessionFiles/diff-A-" + requestId + ".txt");
                BufferedWriter writerA = new BufferedWriter(fileWriterA);
                FileWriter fileWriterB = new FileWriter("./.sessionFiles/diff-B-" + requestId + ".txt");
                BufferedWriter writerB = new BufferedWriter(fileWriterB)
        ) {
            String lineA = readerA.readLine();
            String lineB = readerB.readLine();
            while (true) {
                if (lineA == null && lineB == null) {
                    break;
                } else if (lineA == null) {
                    writerB.write(lineB + "\n");
                    lineB = readerB.readLine();
                } else if (lineB == null) {
                    writerA.write(lineA + "\n");
                    lineA = readerA.readLine();
                } else if (lineA != null && lineB != null) {
                    int comp = lineA.compareTo(lineB);
                    if (comp == 0) {
                        lineA = readerA.readLine();
                        lineB = readerB.readLine();
                    } else if (comp < 0) {
                        writerA.write(lineA + "\n");
                        lineA = readerA.readLine();
                    } else if (comp > 0) {
                        writerB.write(lineB + "\n");
                        lineB = readerB.readLine();
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            return props.getProperty("INVALID_REQUEST_ID");
        }

        log.info("DIFF_DONE");
        return "DIFF_DONE";
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
                    props.getProperty("GETNEXT_QUERY") +
                            prefixKey +
                            " count: " +
                            count +
                            " requestId: " +
                            requestId
            );
            assert count > 0;
        } catch (Exception e) {
            log.error(e.getMessage());
            return props.getProperty("BAD_ARGUMENTS");
        }

        try {
            BotSession botSession = getBotSession(requestId);
            if (!botSession.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTED)) {
                return props.getProperty("TRIES_NOT_CREATED");
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
            return props.getProperty("INVALID_REQUEST_ID");
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
            log.error(props.getProperty("DELETING_ERROR"), requestId);
            return props.getProperty("BAD_ARGUMENTS");
        }
        try {
            log.info(props.getProperty("DELETING_INITIATE"), requestId);
            assert botSession != null;
            for (String file : new String[]{
                    botSession.getKeysA(),
                    botSession.getKeysB(),
                    botSession.getDumpA(),
                    botSession.getDumpB(),
                    botSession.getKeysA() + "sorted.txt",
                    botSession.getKeysB() + "sorted.txt",
                    "./.sessionFiles/diff-A-" + requestId + ".txt",
                    "./.sessionFiles/diff-B-" + requestId + ".txt",
            }) {
                if (Files.exists(Paths.get(file))) {
                    Files.delete(Paths.get(file));
                    log.info(props.getProperty("DELETING_KEYS_SUCCESS"), file);
                } else {
                    log.info(props.getProperty("DELETING_KEYS_ERROR"), file);
                }
            }
        } catch (IOException e) {
            log.error(props.getProperty("DELETING_ERROR"), requestId);
            throw new RuntimeException(e);
        }
        removeBotSession(requestId);
        return props.getProperty("DELETING_SUCCESS") + requestId;
    }

    /**
     * Deletes all active sessions
     *
     * @return String containing the delete-success message to be sent to the channels
     */
    public static String deleteAllSessionsUtils() {
        BotSession.getAllBotSessions().forEach((key, value) -> deleteSessionUtils(key));
        return props.getProperty("DELETING_ALL_SUCCESS");
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
            final int[] index = {1};
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
            return props.getProperty("QUERYING_NOT_POSSIBLE");
        }
        return props.getProperty("QUERYING_POSSIBLE");
    }
}
