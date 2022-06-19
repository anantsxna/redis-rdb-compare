package org.example;

import static org.example.Channel.*;
import static org.messaging.PostUpdate.postTextResponseAsync;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
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
        "Please provide proper arguments.\nRefer to \"/redis-bot-help\" for more information.";
    private static final String SESSION_IN_PROGRESS =
        "A session is already open in this channel.\n";
    private static final String SESSION_CREATED =
        "A session has been created in this channel. Ready to parse and make tries.\n";
    private static final String QUERYING_NOT_POSSIBLE =
        "Querying is not possible since tries have not been created.\n";
    private static final String QUERYING_POSSIBLE =
        "Querying is possible since tries have been created.\n";

    /**
     * Create a channel with the given channelId.
     * @param channelId: the channel to create
     * @return true if channel was created, false if channel already exists
     */
    public static String createSessionUtils(final String channelId) {
        if (createChannel(channelId)) {
            return SESSION_CREATED;
        } else {
            return SESSION_IN_PROGRESS;
        }
    }

    /**
     * Checks if the downloading is in progress or not and if not started, execute downloading in parallel
     * This method is only invoked by the command "/download". For interactive downloading, there is no method yet.
     * @param channelId: the channel to check the status of
     * @return downloading status of the channel
     */
    public static String downloadUtils(String text, final String channelId) {
        Channel channel = getChannel(channelId);
        URL[] urls;

        try {
            assert (!text.isEmpty());
            assert (!text.contains(" "));
            String[] args = text.split(" ");
            assert (args.length == 2);
            channel.setS3linkA(new URL(args[0]));
            channel.setS3linkB(new URL(args[1]));
        } catch (Exception e) {
            //TODO : expand reasons for invalid query
            return BAD_ARGUMENTS;
        }

        if (channel.getExecutedDownloading().compareAndSet(false, true)) {
            log.info("downloading started for channel {}", channelId);
            channel
                .getDownloadingExecutorService()
                .submit(() -> {
                    channel.setDownloadingStatus(DownloadingStatus.DOWNLOADING);
                    Downloader downloader = channel.getDownloader();
                    downloader.addToDownloader(channel.getS3linkA(), channel.getDumpA());
                    downloader.addToDownloader(channel.getS3linkB(), channel.getDumpB());

                    long startTime = System.currentTimeMillis();
                    try {
                        boolean terminatedWithSuccess = channel.getDownloader().download();
                        if (!terminatedWithSuccess) {
                            throw new InterruptedException("Timeout Exception while downloading");
                        }
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                        log.error(
                            "download interrupted due to FileNotFound, SecurityException or DownloadingError for channel {}",
                            channelId
                        );
                        channel.setDownloadingStatus(DownloadingStatus.NOT_DOWNLOADED);
                        postTextResponseAsync(
                            "\uD83D\uDEA8\uD83D\uDEA8 Download failed \uD83D\uDEA8\uD83D\uDEA8",
                            channelId
                        );
                        throw new RuntimeException(e);
                    }
                    long endTime = System.currentTimeMillis();
                    log.info(
                        "Download completed in {} milliseconds in channel {}",
                        endTime - startTime,
                        channelId
                    );
                    channel.setDownloadingTime(endTime - startTime);
                    channel.setDownloadingStatus(DownloadingStatus.DOWNLOADED); //volatile variable write
                    postTextResponseAsync(
                        "\uD83D\uDEA8\uD83D\uDEA8 Download completed in " +
                        (endTime - startTime) /
                        1000.0 +
                        " second(s). \uD83D\uDEA8\uD83D\uDEA8",
                        channelId
                    );
                });
            channel.getDownloadingExecutorService().shutdown();
            return DOWNLOADING_STARTED;
        } else {
            if (channel.getDownloadingStatus().equals(DownloadingStatus.DOWNLOADING)) {
                return DOWNLOADING_IN_PROGRESS;
            } else if (channel.getDownloadingStatus().equals(DownloadingStatus.DOWNLOADED)) {
                return (
                    DOWNLOADING_COMPLETED +
                    " in " +
                    channel.getDownloadingTime() /
                    1000.0 +
                    " second(s)."
                );
            } else {
                return UNKNOWN_DOWNLOADING_BEHAVIOUR + channel.getDownloadingStatus();
            }
        }
    }

    /**
     * Checks if the parsing is in progress or not and if not started, execute parsing in parallel
     * This method is only invoked by the command "/parse". For interactive parsing, use ParseAndMakeTrieView class
     * @param channelId: the channel to check the status of
     * @return parsing status of the channel
     */
    public static String parseUtils(final String channelId) {
        Channel channel = getChannel(channelId);

        if (!channel.getDownloadingStatus().equals(DownloadingStatus.DOWNLOADED)) {
            return DOWNLOADING_NOT_COMPLETED;
        }

        log.info("/parse called after downloading completed.");

        if (channel.getExecutedParsing().compareAndSet(false, true)) {
            log.info("parsing started for channel {}", channelId);
            channel
                .getParsingExecutorService()
                .submit(() -> {
                    channel.setParsingStatus(ParsingStatus.IN_PROGRESS);
                    Parser parser = channel.getParser();
                    parser.addToParser(channel.getDumpA(), channel.getKeysA());
                    parser.addToParser(channel.getDumpB(), channel.getKeysB());

                    long startTime = System.currentTimeMillis();
                    parser.parse();
                    channel.setParsingStatus(ParsingStatus.COMPLETED); //volatile variable write
                    long endTime = System.currentTimeMillis();

                    log.info(
                        "parsing completed for channel {} in {} ms",
                        channelId,
                        endTime - startTime
                    );
                    channel.setParsingTime(endTime - startTime);
                    postTextResponseAsync(
                        "\uD83D\uDEA8\uD83D\uDEA8 Parsing completed in " +
                        (endTime - startTime) /
                        1000.0 +
                        " second(s). \uD83D\uDEA8\uD83D\uDEA8",
                        channelId
                    );
                });
            channel.getParsingExecutorService().shutdown();
            return PARSING_STARTED;
        } else {
            if (channel.getParsingStatus().equals(ParsingStatus.IN_PROGRESS)) {
                return PARSING_IN_PROGRESS;
            } else if (channel.getParsingStatus().equals(ParsingStatus.COMPLETED)) {
                return (
                    PARSING_COMPLETED + " in " + channel.getParsingTime() / 1000.0 + " second(s)."
                );
            } else {
                return UNKNOWN_PARSING_BEHAVIOUR + channel.getParsingStatus();
            }
        }
    }

    /**
     * Checks if the tries are in progress or not and if not started, execute tries in parallel
     * This method is only invoked by the command "/maketrie". For interactive tries, use ParseAndMakeTrieView class
     * @param channelId: the channel to check the status of
     * @return trie status of the channel
     */
    public static String trieConstructionUtils(final String channelId) {
        Channel channel = getChannel(channelId);
        if (!channel.getParsingStatus().equals(ParsingStatus.COMPLETED)) {
            return PARSING_NOT_COMPLETED;
        }

        if (channel.getExecutedTrieMaking().compareAndSet(false, true)) {
            channel.setTrieMakingStatus(TrieMakingStatus.CONSTRUCTING);
            log.info("trie construction started for channel {}", channelId);
            channel
                .getTrieMakingExecutorService()
                .submit(() -> {
                    channel.setTrieA(QTrie.builder().keysFile(channel.getKeysA()).build());
                    channel.setTrieB(QTrie.builder().keysFile(channel.getKeysB()).build());

                    channel.getTrieMaker().addToTrieMaker(channel.getDumpA(), channel.getTrieA());
                    channel.getTrieMaker().addToTrieMaker(channel.getDumpB(), channel.getTrieB());

                    long startTime = System.currentTimeMillis();
                    try {
                        boolean terminatedWithSuccess = channel.getTrieMaker().makeTries();
                        if (!terminatedWithSuccess) {
                            throw new Exception("Timeout Exception while making tries");
                        }
                    } catch (InterruptedException e) {
                        log.error(
                            "trie construction interrupted due trie-initializer-threads being interrupted for channel {}",
                            channelId
                        );
                        channel.setTrieMakingStatus(TrieMakingStatus.NOT_CONSTRUCTED);
                        postTextResponseAsync(
                            "\uD83D\uDEA8\uD83D\uDEA8 Trie construction failed \uD83D\uDEA8\uD83D\uDEA8",
                            channelId
                        );
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        log.error(
                            "trie construction interrupted due to timeout for channel {}",
                            channelId
                        );
                        channel.setTrieMakingStatus(TrieMakingStatus.NOT_CONSTRUCTED);
                        postTextResponseAsync(
                            "\uD83D\uDEA8\uD83D\uDEA8 Trie construction failed \uD83D\uDEA8\uD83D\uDEA8",
                            channelId
                        );
                        throw new RuntimeException(e);
                    }
                    long endTime = System.currentTimeMillis();
                    log.info(
                        "Trie construction completed in {} milliseconds in channel {}",
                        endTime - startTime,
                        channelId
                    );
                    channel.setTrieMakingTime(endTime - startTime);
                    channel.setTrieMakingStatus(TrieMakingStatus.CONSTRUCTED); //volatile variable write
                    postTextResponseAsync(
                        "\uD83D\uDEA8\uD83D\uDEA8 Trie construction completed in " +
                        (endTime - startTime) /
                        1000.0 +
                        " second(s). \uD83D\uDEA8\uD83D\uDEA8",
                        channelId
                    );
                });
            channel.getTrieMakingExecutorService().shutdown();
            return TRIE_CONSTRUCTION_STARTED;
        } else {
            if (channel.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTING)) {
                return TRIE_CONSTRUCTION_IN_PROGRESS;
            } else if (channel.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTED)) {
                return (
                    TRIE_CONSTRUCTION_COMPLETED +
                    " in " +
                    channel.getTrieMakingTime() /
                    1000.0 +
                    " second(s)."
                );
            } else {
                return UNKNOWN_TRIE_CONSTRUCTION_BEHAVIOUR + channel.getTrieMakingStatus();
            }
        }
    }

    /**
     * Checks if executing queries is possible or not
     * @param channelId: the channel to start the session
     * @return QUERYING_NOT_POSSIBLE if queries cannot be executed, else returns empty string
     */
    public static String queryAllUtils(final String channelId) {
        Channel channel = getChannel(channelId);
        if (!channel.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTED)) {
            return QUERYING_NOT_POSSIBLE;
        }
        return QUERYING_POSSIBLE;
    }

    /**
     * - Check if the "/getcount" query can execute or not
     * - Check if the query arguments are valid or not
     * @param text: the query arguments
     * @param channelId: the channel to check the status of
     * @return String containing the query result or error message
     */
    public static String countUtils(String text, final String channelId) {
        Channel channel = getChannel(channelId);
        if (!channel.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTED)) {
            return TRIES_NOT_CREATED;
        } else {
            try {
                assert (!text.isEmpty());
                assert (!text.contains(" "));
            } catch (Exception e) {
                //TODO : expand reasons for invalid query
                return BAD_ARGUMENTS;
            }
        }
        log.info("Counting for key: " + text);
        Query query = CountQuery
            .builder()
            .key(text)
            .queryType(Query.QueryType.GET_COUNT)
            .channelId(channelId)
            .build();
        query.execute();
        return query.result();
    }

    /**
     * - Check if the "/getcount" query can execute or not
     * - Check if the query arguments are valid or not
     *
     * @param text: the query arguments
     * @param channelId: the channel to check the status of
     * @return String containing the query result or error message
     */
    public static String getNextKeyUtils(String text, final String channelId) {
        String key = "";
        int count = 1;
        Channel channel = getChannel(channelId);
        if (!channel.getTrieMakingStatus().equals(TrieMakingStatus.CONSTRUCTED)) {
            return TRIES_NOT_CREATED;
        } else {
            try {
                assert (!text.isEmpty());
                String[] tokens = text.split(" ");
                key = tokens[0];
                count = Integer.parseInt(tokens[1]);
                assert (tokens.length == 2);
                assert (count > 0);
            } catch (Exception e) {
                return BAD_ARGUMENTS;
            }
        }

        Query query = NextKeyQuery
            .builder()
            .key(key)
            .n(count)
            .queryType(NextKeyQuery.QueryType.TOP_K_CHILDREN)
            .channelId(channelId)
            .build();
        query.execute();
        return query.result();
    }

    /**
     * Clears the given channel's data by:
     * - removing the channel from the list of channels
     * - deleting the channel's dump file and keys files (TODO: InputRDB)
     * @param channelId: the channel to clear the session in
     * @return String containing the delete-success message to be sent to the channel
     */
    public static String deleteSessionUtils(final String channelId) {
        Channel channel = getChannel(channelId);
        try {
            log.info("Deleting channel: " + channelId);
            if (Files.exists(Paths.get(channel.getDumpA()))) {
                Files.delete(Paths.get(channel.getDumpA()));
                log.info("Deleted dump file: " + channel.getDumpA());
            } else {
                log.info("Cannot delete dump file. Does not exist: " + channel.getDumpA());
            }

            if (Files.exists(Paths.get(channel.getDumpB()))) {
                Files.delete(Paths.get(channel.getDumpB()));
                log.info("Deleted dump file: " + channel.getDumpB());
            } else {
                log.info("Cannot delete dump file. Does not exist: " + channel.getDumpB());
            }

            if (Files.exists(Paths.get(channel.getKeysA()))) {
                Files.delete(Paths.get(channel.getKeysA()));
                log.info("Deleted keys file: " + channel.getKeysA());
            } else {
                log.info("Cannot delete keys file. Does not exist: " + channel.getKeysA());
            }

            if (Files.exists(Paths.get(channel.getKeysB()))) {
                Files.delete(Paths.get(channel.getKeysB()));
                log.info("Deleted keys file: " + channel.getKeysB());
            } else {
                log.info("Cannot delete keys file. Does not exist: " + channel.getKeysB());
            }
        } catch (IOException e) {
            log.error("Error deleting files for channel {}", channelId);
            throw new RuntimeException(e);
        }
        removeChannel(channelId);
        return "Deleted: session for this channel.";
    }

    /**
     * Deletes all active sessions
     * @return String containing the delete-success message to be sent to the channels
     */
    public static String deleteAllSessionsUtils() {
        Channel
            .getChannels()
            .forEach((key, value) -> {
                deleteSessionUtils(key);
            });
        return "Deleted: all sessions.";
    }

    /**
     * List all active sessions
     * @return String containing the list of active sessions with requestId and the s3links
     */
    public static String listSessionsUtils() {
        StringBuilder sb = new StringBuilder();
        if (Channel.getChannels().isEmpty()) {
            sb.append(">No sessions are active.");
        } else {
            sb.append("Active sessions: \n\n");
            final int[] index = { 1 };
            Channel
                .getChannels()
                .forEach((key, channel) -> {
                    sb
                        .append(index[0])
                        .append(". Request Id: `")
                        .append(channel.getRequestId())
                        .append("`:\n>A: <")
                        .append(channel.getS3linkA().toString())
                        .append("|")
                        .append(Channel.formatLink(channel.getS3linkA().toString()))
                        .append(">\n>B: <")
                        .append(channel.getS3linkB().toString())
                        .append("|")
                        .append(Channel.formatLink(channel.getS3linkB().toString()))
                        .append(">\n\n");
                    index[0]++;
                });
        }
        return sb.toString();
    }

    /**
     * Resets the internal parameters of the channel(or 'session') to their default values.
     * Cheats by deleting the channel and creating a new session.
     * @param channelId: the channel to reset the session in
     */
    public static String resetSessionUtils(final String channelId) {
        Channel channel = getChannel(channelId);
        deleteSessionUtils(channelId);
        createSessionUtils(channelId);
        return "Channel has been reset to default values. Tries and input files deleted. Session state variables reset.";
    }
}
