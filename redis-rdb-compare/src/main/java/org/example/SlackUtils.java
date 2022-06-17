package org.example;

import static org.example.Channel.*;
import static org.messaging.PostUpdate.postTextResponseAsync;

import lombok.extern.slf4j.Slf4j;
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

    private static final String PARSING_NOT_COMPLETED =
        "Parsing not done. Please wait for parsing to finish or use \"/parse\" command to start parsing.";
    private static final String PARSING_STARTED =
        "Parsing has started...\nPlease wait for automatic notification when parsing is done.\nOr use \"/parse\" command again to check status.";
    private static final String PARSING_IN_PROGRESS = "Parsing in progress.\nPlease wait.";
    private static final String PARSING_COMPLETED = "Parsing completed";
    private static final String TRIES_NOT_CREATED =
        "Tries not created.\nPlease wait for tries to be created\nOr use \"/maketrie\" command to start creating tries.";
    private static final String TRIE_CONSTRUCTION_STARTED =
        "Trie construction started...\nPlease wait for automatic notification when construction is over.\nOr use \"/maketrie\" command again to check status.";
    private static final String TRIE_CONSTRUCTION_IN_PROGRESS =
        "Trie construction in progress.\nPlease wait.";
    private static final String TRIE_CONSTRUCTION_COMPLETED = "Trie construction completed";
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
    private static final String DOWNLOADING_NOT_COMPLETED =
        "Downloading not completed.\nPlease wait for downloading to finish.";

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
     * Checks if executing queries is possible or not
     * @param channelId: the channel to start the session
     * @return QUERYING_NOT_POSSIBLE if queries cannot be executed, else returns empty string
     */
    public static String queryAllUtils(final String channelId) {
        Channel channel = getChannel(channelId);
        if (!channel.getTrieStatus().equals(TrieStatus.CONSTRUCTED)) {
            return QUERYING_NOT_POSSIBLE;
        }
        return QUERYING_POSSIBLE;
    }

    /**
     * Clears the given channel's data by:
     * - removing the channel from the list of channels
     * - deleting the channel's dump file and keys files (TODO: InputRDB)
     * @param channelId: the channel to clear the session in
     * @return String containing the delete-success message to be sent to the channel
     */
    public static String deleteSessionUtils(final String channelId) {
        removeChannel(channelId);
        return "Deleted: session for this channel.";
    }

    /**
     * Resets the internal parameters of the channel(or 'session') to their default values.
     * @param channelId: the channel to reset the session in
     */
    public static String resetSessionUtils(final String channelId) {
        Channel channel = getChannel(channelId);
        channel.resetChannel();
        return "Channel has been reset to default values. Tries and input files deleted. Session state variables reset.";
    }

    /**
     * Checks if the parsing is in progress or not and if not started, execute parsing in parallel
     * This method is only invoked by the command "/parse". For interactive parsing, use ParseAndMakeTrieView class
     * @param channelId: the channel to check the status of
     * @return parsing status of the channel
     */
    public static String parseUtils(final String channelId) {
        Channel channel = getChannel(channelId);

        if (!channel.getFileStatus().equals(FileStatus.DOWNLOADED)) {
            return DOWNLOADING_NOT_COMPLETED;
        }

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
                return "parseUtils() is showing UNKNOWN behaviour: " + channel.getParsingStatus();
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

        if (channel.getExecutedTrie().compareAndSet(false, true)) {
            channel.setTrieStatus(TrieStatus.CONSTRUCTING);
            log.info("trie construction started for channel {}", channelId);
            channel
                .getMakeTrieExecutorService()
                .submit(() -> {
                    channel.setTrieA(QTrie.builder().keysFile(channel.getKeysA()).build());
                    channel.setTrieB(QTrie.builder().keysFile(channel.getKeysB()).build());

                    channel.getTrieMaker().addToTrieMaker(channel.getDumpA(), channel.getTrieA());
                    channel.getTrieMaker().addToTrieMaker(channel.getDumpB(), channel.getTrieB());

                    long startTime = System.currentTimeMillis();
                    try {
                        boolean terminatedWithSuccess = channel.getTrieMaker().makeTries();
                        if (!terminatedWithSuccess) {
                            throw new Exception("Timeout Exception");
                        }
                    } catch (InterruptedException e) {
                        log.error(
                            "trie construction interrupted due trie-initializer-threads being interrupted for channel {}",
                            channelId
                        );
                        channel.setTrieStatus(TrieStatus.NOT_CONSTRUCTED);
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
                        channel.setTrieStatus(TrieStatus.NOT_CONSTRUCTED);
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
                    channel.setMakeTrieTime(endTime - startTime);
                    channel.setTrieStatus(TrieStatus.CONSTRUCTED); //volatile variable write
                    postTextResponseAsync(
                        "\uD83D\uDEA8\uD83D\uDEA8 Trie construction completed in " +
                        (endTime - startTime) /
                        1000.0 +
                        " second(s). \uD83D\uDEA8\uD83D\uDEA8",
                        channelId
                    );
                });
            channel.getMakeTrieExecutorService().shutdown();
            return TRIE_CONSTRUCTION_STARTED;
        } else {
            if (channel.getTrieStatus().equals(TrieStatus.CONSTRUCTING)) {
                return TRIE_CONSTRUCTION_IN_PROGRESS;
            } else if (channel.getTrieStatus().equals(TrieStatus.CONSTRUCTED)) {
                return (
                    TRIE_CONSTRUCTION_COMPLETED +
                    " in " +
                    channel.getMakeTrieTime() /
                    1000.0 +
                    " second(s)."
                );
            } else {
                return "trieConstructUtils() is showing UNKNOWN behaviour";
            }
        }
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
        if (!channel.getTrieStatus().equals(TrieStatus.CONSTRUCTED)) {
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
        if (!channel.getTrieStatus().equals(TrieStatus.CONSTRUCTED)) {
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
}
