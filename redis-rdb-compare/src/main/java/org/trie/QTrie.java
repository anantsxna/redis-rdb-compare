package org.trie;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.Main;

/**
 * Trie class.
 */
@Slf4j
@Builder
public final class QTrie {

    @Builder.Default
    @Getter
    private final TrieNode root = TrieNode.builder().build();

    @Builder.Default
    private final Properties props = Main.props;

    @Getter
    @Setter
    @Builder.Default
    private static String DELIMITER = ":";

    @NonNull
    private final String keysFile;

    @NonNull
    private final String requestId;

    @NonNull
    @Builder.Default
    private Integer nodesCreated = 0;

    /**
     * Reads keys from file and inserts them into trie.
     */
    public void takeInput() {
        DELIMITER = props.getProperty("DELIMITER");
        try (
                FileReader fileReader = new FileReader(keysFile);
                BufferedReader reader = new BufferedReader(fileReader)
        ) {
            log.info("Reading keys from file: {}", keysFile);
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                insertKey(line);
                i++;
                if (i % 10000 == 0 && keysFile.contains("A")) {
                    log.info("{} {}", i, nodesCreated);
                    //                    log.info("Read " + i + " keys.");
                }
                //                int finalI = i;
                //                stops.forEach(stop -> {
                //                    if (finalI == stop) {
                //                        log.info("@ nodes created: {} {}", finalI, nodesCreated);
                //                    }
                //                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Inserts a key into the trie.
     *
     * @param dbKey: key to be inserted.
     */
    public void insertKey(String dbKey) {
        BotStringTokenizer tokenizer = BotStringTokenizer
                .builder()
                .path(dbKey)
                .delimiter(DELIMITER)
                .build()
                .tokenize();
        // log.info("Inserting key: {}", dbKey);
        TrieNode current = root;

        //        String startsWith = tokenizer.startsWith();
        //        if (startsWith != null) {
        //            BotSession botSession = BotSession.getBotSession(requestId);
        //            assert botSession != null;
        //            botSession.getParentKeys().add(startsWith);
        //        }

        while (tokenizer.hasMoreTokens()) {
            current.addCount();
            String nextKey = tokenizer.nextToken();
            //            log.info("Next key: {}", nextKey);
            if (tokenizer.hasMoreTokens()) {
                if (!current.hasChild(nextKey)) {
                    current.addChild(nextKey);
                    nodesCreated++;
                }
                current = current.getChild(nextKey);
            } else break;
        }
    }

    /**
     * Returns the 'n' maximum count child nodes of a node that represents the given prefix.
     *
     * @param _prefix: prefix to be searched.
     * @param n:       number of child nodes to be returned.
     * @return List of (n+2) pairs, where:
     * the first pair has the total number of keys with the prefix '_prefix'.
     * the second pair has the total number of child nodes of the node that represents the prefix '_prefix'.
     * each of the next n pairs contains the child keys in decreasing order of their count.
     * If the number of child nodes is less than n, List has less than (n+2) pairs.
     */
    public List<Map.Entry<String, Integer>> topNKeyWithPrefix(String _prefix, int n)
            throws Exception {
        final String prefix = _prefix.replaceFirst(DELIMITER + "$", "");

        List<Map.Entry<String, Integer>> result = new ArrayList<>();
        TrieNode node = traverseTrie(prefix);
        if (node == null) {
            throw new Exception("Prefix not found.");
        }

        result.add(new AbstractMap.SimpleEntry<>(prefix + " total keys", node.getCount()));
        result.add(
                new AbstractMap.SimpleEntry<>(prefix + " total children", node.getChildrenCount())
        );

        node
                .getChildren()
                .entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue().getCount() - e1.getValue().getCount())
                .limit(n)
                .forEach(e -> result.add(getKeyAndCountOutput(e, prefix)));

        return result;
    }

    public Set<String> getChildren(String prefix) {
        if (prefix.compareTo("") == 0) {
            return root.getChildren().keySet();
        }
        TrieNode node = traverseTrie(prefix);
        if (node == null) {
            return new HashSet<>();
        }
        Set<String> children = new HashSet<>();
        node
                .getChildren()
                .keySet()
                .forEach(child -> {
                    children.add(prefix + DELIMITER + child);
                });

        return children;
    }

    /**
     * Returns the full key and count of a child node.
     *
     * @param _entry: Pair<Key Suffix, child TrieNode>.
     * @param prefix: prefix of the parent node.
     * @return Pair<full key, count of the key>.
     */
    private Map.Entry<String, Integer> getKeyAndCountOutput(
            Map.Entry<String, TrieNode> _entry,
            String prefix
    ) {
        return Map.entry(
                prefix.concat(DELIMITER).concat(_entry.getKey()),
                _entry.getValue().getCount()
        );
    }

    /**
     * Returns the count of a node that represents the given key.
     *
     * @param _prefix: key to be searched.
     * @return an integer representing the count of the node.
     * @throws Exception: if the key is not found in the trie.
     */
    public Integer getCountForPrefix(String _prefix) {
        final String prefix = _prefix.replaceFirst(DELIMITER + "$", "");
        TrieNode node = traverseTrie(prefix);
        if (node == null) {
            return 0;
        }
        return node.getCount();
    }

    /**
     * Traverse the trie from the root node to the node that represents the given path.
     *
     * @param path: path to be traversed.
     * @return TrieNode that represents the given path.
     * @throws Exception: if the path is not found in the trie.
     */
    private TrieNode traverseTrie(String path) {
        BotStringTokenizer tokenizer = BotStringTokenizer
                .builder()
                .path(path)
                .delimiter(DELIMITER)
                .build()
                .tokenize();
        TrieNode current = root;
        while (tokenizer.hasMoreTokens()) {
            String nextKey = tokenizer.nextToken();
            if (!current.hasChild(nextKey)) {
                return null;
            }
            current = current.getChild(nextKey);
        }
        return current;
    }
}
