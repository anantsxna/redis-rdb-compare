package org.example;

import com.slack.api.model.block.LayoutBlock;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processing.Parser;
import org.querying.Query;
import org.querying.countQuery;
import org.querying.nextKeyQuery;
import org.trie.QTrie;

// TODO: Change all return methods from String to List<LayoutBlock>
public class SlackHelper {

    private static final String dumpA = "../dump-A.rdb";
    private static final String dumpB = "../dump-B.rdb";
    private static final String keysA = "../keys-A.txt";
    private static final String keysB = "../keys-B.txt";
    public static QTrie trieA = null;
    public static QTrie trieB = null;
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
    private static final Logger logger = LogManager.getLogger(SlackHelper.class);

    private enum ParsingStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
    }

    private enum TrieStatus {
        NOT_CONSTRUCTED,
        CONSTRUCTING,
        CONSTRUCTED,
    }

    private static volatile ParsingStatus parsingStatus = ParsingStatus.NOT_STARTED;
    private static volatile TrieStatus trieStatus = TrieStatus.NOT_CONSTRUCTED;

    public static String parseUtils() {
        if (parsingStatus == ParsingStatus.IN_PROGRESS) {
            return PARSING_IN_PROGRESS;
        } else if (parsingStatus == ParsingStatus.COMPLETED) {
            return PARSING_COMPLETED;
            //TODO: ask user for intent, reset parsingStatus to NOT_STARTED and skip return
        }

        //TODO: ask user for input regarding file location
        //TODO: download files from s3 link and save to local directory
        Parser.clear();
        Parser.addToParser(dumpA, keysA);
        Parser.addToParser(dumpB, keysB);
        new Thread(() -> {
            Parser.parse();
            parsingStatus = ParsingStatus.COMPLETED;
            //TODO: Find a way to send a message to the user that the parsing is completed
        })
            .start();
        parsingStatus = ParsingStatus.IN_PROGRESS;
        return PARSING_STARTED;
    }

    public static String trieConstructionUtils() {
        if (parsingStatus != ParsingStatus.COMPLETED) {
            return PARSING_NOT_COMPLETED;
        }

        if (trieStatus == TrieStatus.CONSTRUCTING) {
            return TRIE_CONSTRUCTION_IN_PROGRESS;
        }

        if (trieStatus == TrieStatus.CONSTRUCTED) {
            return TRIE_CONSTRUCTION_COMPLETED;
        }

        trieStatus = TrieStatus.CONSTRUCTING;
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            trieA = new QTrie(keysA);
            trieB = new QTrie(keysB);
            long endTime = System.currentTimeMillis();
            System.out.println(
                "Trie construction completed in " + (endTime - startTime) + " milliseconds"
            );
            trieStatus = TrieStatus.CONSTRUCTED;
        })
            .start();
        return TRIE_CONSTRUCTION_STARTED;
    }

    public static String countUtils(String text) {
        if (trieStatus != TrieStatus.CONSTRUCTED) {
            return TRIES_NOT_CREATED;
        } else {
            // TODO: implement syntax checking of the key
        }
        Query query = new countQuery(text);
        query.execute();
        return query.result();
    }

    public static String getNextKeyUtils(String text) {
        String key = "";
        int count = 1;
        if (trieStatus != TrieStatus.CONSTRUCTED) {
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
        Query query = new nextKeyQuery(key, count);
        query.execute();
        return query.result();
    }
}
