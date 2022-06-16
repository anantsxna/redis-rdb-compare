package org.example;

import static org.example.Channel.getChannel;
import static org.example.Channel.removeChannel;

import lombok.extern.slf4j.Slf4j;
import org.example.Channel.ParsingStatus;
import org.example.Channel.TrieStatus;
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
        "Parsing has started...\nPlease wait.\nUse \"/parse\" command again to check status.";
    private static final String PARSING_IN_PROGRESS = "Parsing in progress.\nPlease wait.";
    private static final String PARSING_COMPLETED = "Parsing completed.";
    private static final String TRIES_NOT_CREATED =
        "Tries not created.\nPlease wait for tries to be created\nOr use \"/maketrie\" command to start creating tries.";
    private static final String TRIE_CONSTRUCTION_STARTED =
        "Trie construction started...\nPlease wait.\nUse \"/maketrie\" command again to check status.";
    private static final String TRIE_CONSTRUCTION_IN_PROGRESS =
        "Trie construction in progress.\nPlease wait.";
    private static final String TRIE_CONSTRUCTION_COMPLETED = "Trie construction completed.";
    private static final String BAD_ARGUMENTS =
        "Please provide proper arguments.\nRefer to \"/redis-bot-help\" for more information.";
    private static final String SESSION_IN_PROGRESS =
        "A session is already open in this channel.\n";
    private static final String QUERYING_NOT_POSSIBLE =
        "Querying is not possible since tries have not been created.\n";

    /**
     * Checks if the interactive session can start or not
     * @param channelId: the channel to start the session
     * @return SESSION_IN_PROGRESS if session cannot start, else returns empty string
     */
    public static String startAllUtils(final String channelId) {
        Channel channel = getChannel(channelId);
        if (!channel.getParsingStatus().equals(ParsingStatus.NOT_STARTED)) {
            return SESSION_IN_PROGRESS;
        }
        //TODO: return session available message, not empty
        // add check in startAll blockAction accordingly
        return "";
    }

    /**
     * Checks if executing queries is possible or not
     * @param channelId: the channel to start the session
     * @return QUERYING_NOT_POSSIBLE if queries cannot be executed, else returns empty string
     */
    public static String queryAllUtils(final String channelId) {
        Channel channel = getChannel(channelId);
        if (!channel.trieStatus.equals(TrieStatus.CONSTRUCTED)) {
            return QUERYING_NOT_POSSIBLE;
        }
        //TODO: return session available message, not empty
        // add check in startAll blockAction accordingly
        return "";
    }

    /**
     * Clears the given channel's data by:
     * - removing the channel from the list of channels
     * - deleting the channel's dump file and keys files (TODO: InputRDB)
     * @param channelId: the channel to clear the session in
     * @return String containing the delete-success message to be sent to the channel
     */
    public static String clearUtils(final String channelId) {
        removeChannel(channelId);
        return "Deleted: bot files for this channel.";
    }

    /**
     * Checks if the parsing is in progress or not and if not started, execute parsing in parallel
     * This method is only invoked by the command "/parse". For interactive parsing, use ParseAndMakeTrieView class
     * @param channelId: the channel to check the status of
     * @return parsing status of the channel
     */
    public static String parseUtils(final String channelId) {
        Channel channel = getChannel(channelId);
        channel.getParseLock().readLock().lock();
        try {
            if (channel.getParsingStatus().equals(ParsingStatus.IN_PROGRESS)) {
                return PARSING_IN_PROGRESS;
            } else if (channel.getParsingStatus().equals(ParsingStatus.COMPLETED)) {
                return PARSING_COMPLETED;
            }
        } finally {
            channel.getParseLock().readLock().unlock();
        }

        //TODO: ask user for input regarding file location,
        // download files from s3 link and save to local directory,
        // set dumpA and dumpB
        channel.getParseLock().writeLock().lock();
        try {
            channel.setParsingStatus(ParsingStatus.IN_PROGRESS);
        } finally {
            channel.getParseLock().writeLock().unlock();
        }

        Parser parser = channel.getParser();
        parser.clear();
        parser.addToParser(channel.getDumpA(), channel.getKeysA());
        parser.addToParser(channel.getDumpB(), channel.getKeysB());
        new Thread(() -> {
            parser.parse();
            channel.getParseLock().writeLock().lock();
            try {
                channel.setParsingStatus(ParsingStatus.COMPLETED); //volatile variable write
            } finally {
                channel.getParseLock().writeLock().unlock();
            }
        })
            .start();

        return PARSING_STARTED;
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

        if (channel.trieStatus.equals(TrieStatus.CONSTRUCTING)) {
            return TRIE_CONSTRUCTION_IN_PROGRESS;
        }

        if (channel.trieStatus.equals(TrieStatus.CONSTRUCTED)) {
            return TRIE_CONSTRUCTION_COMPLETED;
        }

        channel.trieStatus = TrieStatus.CONSTRUCTING;
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            channel.setTrieA(QTrie.builder().keysFile(channel.getKeysA()).build());
            channel.setTrieB(QTrie.builder().keysFile(channel.getKeysB()).build());
            channel.getTrieA().takeInput();
            channel.getTrieB().takeInput();
            long endTime = System.currentTimeMillis();
            log.info(
                "Trie construction completed in {} milliseconds in channel {}",
                endTime - startTime,
                channelId
            );
            channel.trieStatus = TrieStatus.CONSTRUCTED; //volatile variable write
        })
            .start();
        return TRIE_CONSTRUCTION_STARTED;
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
        if (!channel.trieStatus.equals(TrieStatus.CONSTRUCTED)) {
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
        if (!channel.trieStatus.equals(TrieStatus.CONSTRUCTED)) {
            return TRIES_NOT_CREATED;
        } else {
            try {
                assert (!text.isEmpty());
                String[] tokens = text.split(" ");
                key = tokens[0];
                count = Integer.parseInt(tokens[1]);
                assert (tokens.length == 2);
            } catch (Exception e) {
                //TODO : expand reasons for invalid query
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
