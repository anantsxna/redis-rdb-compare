package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.processing.Parser;
import org.trie.QTrie;

/**
 * LocalMain class.
 * Driver class for locally running the application features without the use of slack.
 */
@Slf4j
public class LocalMain {

    static String dumpA = "../dump-A-200M.rdb";
    static String dumpB = "../dump-B-200M.rdb";
    static String keysA = "../keys-A.txt";
    static String keysB = "../keys-B.txt";
    private static final BufferedReader reader = new BufferedReader(
        new InputStreamReader(System.in)
    );

    /**
     * Driver Code for locally running without slack
     */
    public static void main(String[] args) {
        System.out.println("Program start");

        try {
            System.out.println(
                "Enter the path of the first .rdb file (or press enter for default):"
            );
            String input = reader.readLine();
            if (!input.equals("")) {
                dumpA = input;
            }
            System.out.println(
                "Enter the path of the second .rdb file (or press enter for default):"
            );
            input = reader.readLine();
            if (!input.equals("")) {
                dumpB = input;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Parser parser = Parser.builder().build();
        parser.addToParser(dumpA, keysA);
        parser.addToParser(dumpB, keysB);
        try {
            parser.parse();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        System.out.println("Both .rdb Files Successfully parsed.");
        System.out.println("Keys from " + dumpA + " stored in: " + keysA);
        System.out.println("Keys from " + dumpB + " stored in: " + keysB);

        System.out.println("Trie Creation Started!");
        long startTime = System.currentTimeMillis();
        QTrie trieA = QTrie.builder().keysFile(dumpA).build();
        QTrie trieB = QTrie.builder().keysFile(dumpB).build();
        long endTime = System.currentTimeMillis();
        log.info("Total Tries construction time: " + (endTime - startTime) / 1000.0 + " seconds.");
        System.out.println("Tries created!");

        try {
            while (true) {
                System.out.println("Execute a query? (y/n)");
                String input = reader.readLine();
                if (input.equals("y")) {
                    System.out.println(
                        """
                                    Query Type? (1/2):
                                    1. Get Count of keys that begin with a certain Prefix in both databases
                                    2. Top 'n' key-prefixes that begin with a certain fixed string"""
                    );
                    int queryType = Integer.parseInt(reader.readLine());

                    if (queryType == 1) {
                        System.out.println("Prefix to search for: ");
                        String prefix = reader.readLine();
                        log.info("Query-1: {}", prefix);

                        for (QTrie trie : new QTrie[] { trieA, trieB }) {
                            try {
                                int keyCount = trie.getCountForPrefix(prefix);
                                System.out.println(
                                    "Count for " +
                                    prefix +
                                    " in " +
                                    trie.getKeysFile() +
                                    ": " +
                                    keyCount
                                );
                            } catch (Exception e) {
                                System.out.println(
                                    "No keys found for " + prefix + " in " + trie.getKeysFile()
                                );
                                log.error(
                                    "Query-1: For prefix {}, Error in getCountForPrefix: {}",
                                    prefix,
                                    e.getMessage()
                                );
                            }
                        }
                    } else if (queryType == 2) {
                        System.out.println("The fixed prefix to search for: ");
                        String prefix = reader.readLine();
                        System.out.println("Number of prefixes to return: ");
                        int n = Integer.parseInt(reader.readLine());
                        log.info("Query-2: {}, {} keys", prefix, n);

                        for (QTrie trie : new QTrie[] { trieA, trieB }) {
                            try {
                                List<Map.Entry<String, Integer>> query = trie.topNKeyWithPrefix(
                                    prefix,
                                    n
                                );
                                int found = query.size() - 2;
                                if (found < n) {
                                    System.out.println(
                                        "Found " +
                                        found +
                                        " prefixes only, less than the requested number of prefixes.\n"
                                    );
                                }
                                log.info(
                                    "Top {} keys-prefixes with fixed prefix {} found in file {}: ",
                                    found,
                                    prefix,
                                    trie.getKeysFile()
                                );
                                System.out.println(
                                    "Total keys with prefix: " +
                                    prefix +
                                    " in " +
                                    trie.getKeysFile() +
                                    ": " +
                                    query.get(0).getValue()
                                );
                                System.out.println(
                                    "Top " +
                                    found +
                                    " key-prefixes with prefix: \"" +
                                    prefix +
                                    "\": "
                                );
                                for (int i = 2; i < query.size(); i++) {
                                    System.out.println(
                                        (i - 1) +
                                        ". " +
                                        query.get(i).getKey() +
                                        " : " +
                                        query.get(i).getValue() +
                                        " keys."
                                    );
                                }
                                if (query.get(1).getValue() > found) {
                                    System.out.println(
                                        "... and " +
                                        (query.get(1).getValue() - found) +
                                        " more key-prefixes."
                                    );
                                }
                            } catch (Exception e) {
                                System.out.println(
                                    "No keys found for " + prefix + " in " + trie.getKeysFile()
                                );
                                log.error(
                                    "Query-2: For fixed prefix {}, Error in topNKeyWithPrefix: {}",
                                    prefix,
                                    e.getMessage()
                                );
                            }
                        }
                    } else {
                        System.out.println("Invalid Query Type!");
                        break;
                    }
                } else if (input.equals("n")) {
                    break;
                } else {
                    System.out.println("Invalid Input!");
                }
            }
            System.out.println("Program terminated successfully.");
            log.info("Program terminated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
