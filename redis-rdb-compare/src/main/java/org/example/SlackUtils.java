package org.example;

import static org.example.Channel.getChannel;
import static org.example.Channel.removeChannel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.Channel.ParsingStatus;
import org.example.Channel.TrieStatus;
import org.processing.Parser;
import org.querying.Query;
import org.querying.countQuery;
import org.querying.nextKeyQuery;
import org.trie.QTrie;

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

    private static final Logger logger = LogManager.getLogger(SlackUtils.class);

    public static String startAllUtils(final String channelId) {
        Channel channel = getChannel(channelId);
        if (!channel.parsingStatus.equals(ParsingStatus.NOT_STARTED)) {
            return SESSION_IN_PROGRESS;
        }
        return "";
    }

    public static String queryAllUtils(final String channelId) {
        Channel channel = getChannel(channelId);
        if (!channel.trieStatus.equals(TrieStatus.CONSTRUCTED)) {
            return QUERYING_NOT_POSSIBLE;
        }
        return "";
    }

    public static String clearUtils(final String channelId) {
        removeChannel(channelId);
        return "Deleted: bot files for this channel.";
    }

    public static String parseUtils(final String channelId) {
        Channel channel = getChannel(channelId);
        if (channel.parsingStatus.equals(ParsingStatus.IN_PROGRESS)) {
            return PARSING_IN_PROGRESS;
        } else if (channel.parsingStatus.equals(ParsingStatus.COMPLETED)) {
            return PARSING_COMPLETED;
            //TODO: ask user for intent, reset parsingStatus to NOT_STARTED and skip return
        }

        //TODO: ask user for input regarding file location
        //TODO: download files from s3 link and save to local directory
        Parser parser = channel.getParser();
        parser.clear();
        parser.addToParser(channel.getDumpA(), channel.getKeysA());
        parser.addToParser(channel.getDumpB(), channel.getKeysB());
        new Thread(() -> {
            parser.parse();
            channel.parsingStatus = ParsingStatus.COMPLETED;
            //TODO: send a message to the user that the parsing is completed
        })
            .start();
        channel.parsingStatus = ParsingStatus.IN_PROGRESS;
        return PARSING_STARTED;
    }

    public static String trieConstructionUtils(final String channelId) {
        Channel channel = getChannel(channelId);
        if (!channel.parsingStatus.equals(ParsingStatus.COMPLETED)) {
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
            //TODO: send a message to the user that trie construction is completed
            System.out.println(
                "Trie construction completed in " + (endTime - startTime) + " milliseconds"
            );
            channel.trieStatus = TrieStatus.CONSTRUCTED;
        })
            .start();
        return TRIE_CONSTRUCTION_STARTED;
    }

    public static String countUtils(String text, final String channelId) {
        Channel channel = getChannel(channelId);
        if (!channel.trieStatus.equals(TrieStatus.CONSTRUCTED)) {
            return TRIES_NOT_CREATED;
        } else {
            // TODO: implement syntax checking of the key

        }
        System.out.println("Counting for key: " + text);
        Query query = countQuery
            .builder()
            .key(text)
            .queryType(Query.QueryType.GET_COUNT)
            .startTime(System.currentTimeMillis())
            .channelId(channelId)
            .result(new StringBuilder())
            .exitCode(-1)
            .build();
        query.execute();
        return query.result();
    }

    public static String getNextKeyUtils(String text, final String channelId) {
        String key = "";
        int count = 1;
        Channel channel = getChannel(channelId);
        if (!channel.trieStatus.equals(TrieStatus.CONSTRUCTED)) {
            return TRIES_NOT_CREATED;
        } else if (text.isEmpty()) {
            return BAD_ARGUMENTS;
        } else {
            // TODO: implement syntax checking of the key
        }
        try {
            String[] tokens = text.split(" ");
            key = tokens[0];
            count = Integer.parseInt(tokens[1]);
            assert (tokens.length == 2);
        } catch (Exception e) {
            return BAD_ARGUMENTS;
        }
        Query query = nextKeyQuery
            .builder()
            .key(key)
            .n(count)
            .queryType(nextKeyQuery.QueryType.TOP_K_CHILDREN)
            .startTime(System.currentTimeMillis())
            .channelId(channelId)
            .result(new StringBuilder())
            .exitCode(-1)
            .build();
        query.execute();
        return query.result();
    }
}
