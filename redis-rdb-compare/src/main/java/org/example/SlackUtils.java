package org.example;

import static org.example.Channel.getChannel;

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
    public static final String PARSING_COMPLETED = "Parsing completed.";
    private static final String TRIES_NOT_CREATED =
        "Tries not created.\nPlease wait for tries to be created\nOr use \"/maketrie\" command to start creating tries.";
    private static final String TRIE_CONSTRUCTION_STARTED =
        "Trie construction started...\nPlease wait.\nUse \"/maketrie\" command again to check status.";
    private static final String TRIE_CONSTRUCTION_IN_PROGRESS =
        "Trie construction in progress.\nPlease wait.";
    private static final String TRIE_CONSTRUCTION_COMPLETED = "Trie construction completed.";
    private static final String BAD_ARGUMENTS =
        "Please provide proper arguments.\nRefer to \"/redis-bot-help\" for more information.";
    private static final Logger logger = LogManager.getLogger(SlackUtils.class);

    public static String parseUtils(String channelId) {
        Channel channel = getChannel(channelId);
        if (channel.parsingStatus == ParsingStatus.IN_PROGRESS) {
            return PARSING_IN_PROGRESS;
        } else if (channel.parsingStatus == ParsingStatus.COMPLETED) {
            return PARSING_COMPLETED;
            //TODO: ask user for intent, reset parsingStatus to NOT_STARTED and skip return
        }

        //TODO: ask user for input regarding file location
        //TODO: download files from s3 link and save to local directory
        Parser parser = channel.parser;
        parser.clear();
        parser.addToParser(channel.dumpA, channel.keysA);
        parser.addToParser(channel.dumpB, channel.keysB);
        new Thread(() -> {
            parser.parse();
            channel.parsingStatus = ParsingStatus.COMPLETED;
            //TODO: Find a way to send a message to the user that the parsing is completed
        })
            .start();
        channel.parsingStatus = ParsingStatus.IN_PROGRESS;
        return PARSING_STARTED;
    }

    public static String trieConstructionUtils(String channelId) {
        Channel channel = getChannel(channelId);
        if (channel.parsingStatus != ParsingStatus.COMPLETED) {
            return PARSING_NOT_COMPLETED;
        }

        if (channel.trieStatus == TrieStatus.CONSTRUCTING) {
            return TRIE_CONSTRUCTION_IN_PROGRESS;
        }

        if (channel.trieStatus == TrieStatus.CONSTRUCTED) {
            return TRIE_CONSTRUCTION_COMPLETED;
        }

        channel.trieStatus = TrieStatus.CONSTRUCTING;
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            channel.trieA = new QTrie(channel.keysA);
            channel.trieB = new QTrie(channel.keysB);
            long endTime = System.currentTimeMillis();
            //TODO: Add slackpost command here
            System.out.println(
                "Trie construction completed in " + (endTime - startTime) + " milliseconds"
            );
            channel.trieStatus = TrieStatus.CONSTRUCTED;
        })
            .start();
        return TRIE_CONSTRUCTION_STARTED;
    }

    public static String countUtils(String channelId, String text) {
        Channel channel = getChannel(channelId);
        if (channel.trieStatus != TrieStatus.CONSTRUCTED) {
            return TRIES_NOT_CREATED;
        } else {
            // TODO: implement syntax checking of the key
        }
        Query query = new countQuery(text, channelId);
        query.execute();
        return query.result();
    }

    public static String getNextKeyUtils(String channelId, String text) {
        String key = "";
        int count = 1;
        Channel channel = getChannel(channelId);
        if (channel.trieStatus != TrieStatus.CONSTRUCTED) {
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
        System.out.println("Key: " + key + " Count: " + count);
        Query query = new nextKeyQuery(key, count, channelId);
        query.execute();
        return query.result();
    }
}
