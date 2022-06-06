package org.trie;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * Trie class.
 */
public final class QTrie {
    private static final Logger logger = LogManager.getLogger(QTrie.class);
    private final TrieNode root;
    private final String keysFile;

    public QTrie(String keysFile) {
        root = new TrieNode();
        this.keysFile = keysFile;
        takeInput();
    }

    public String getKeysFile() {
        return keysFile;
    }

    /**
     * Reads keys from file and inserts them into trie.
     */
    public void takeInput() {
        try (FileReader fileReader = new FileReader(keysFile); BufferedReader reader = new BufferedReader(fileReader)) {
            logger.info("Reading keys from file: {}", keysFile);
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                insertKey(line);
                i++;
                if (i % 100000 == 0) {
                    logger.info("Read " + i + " keys.");
                }
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
        StringTokenizer tokenizer = new StringTokenizer(dbKey, "/");
        TrieNode current = root;
        while (tokenizer.hasMoreTokens()) {
            current.addCount();
            String nextKey = tokenizer.nextToken();
            if (tokenizer.hasMoreTokens()) {
                if (!current.hasChild(nextKey)) {
                    current.addChild(nextKey);
                }
                current = current.getChild(nextKey);
            }
            else break;
        }
    }

    /**
     * Returns the 'n' maximum frequency child nodes of a node that represents the given prefix.
     *
     * @param _prefix: prefix to be searched.
     * @param n: number of child nodes to be returned.
     *
     * @return List of (n+2) pairs, where:
     *      the first pair has the total number of keys with the prefix '_prefix'.
     *      the second pair has the total number of child nodes of the node that represents the prefix '_prefix'.
     *      each of the next n pairs contains the child keys in decreasing order of their frequencies.
     *
     *      If the number of child nodes is less than n, List has less than (n+2) pairs.
     */
    public List<Map.Entry<String, Integer>> topNKeyWithPrefix(String _prefix, int n) throws Exception {
        final String prefix = _prefix.replaceFirst("/$", "");
        if (prefix.isBlank() || prefix.isEmpty()) {
            throw new Exception("Prefix cannot be empty");
        }

        List<Map.Entry<String, Integer>> result = new ArrayList<>();
        TrieNode node = traverseTrie(prefix);

        result.add(new AbstractMap.SimpleEntry<>(prefix + " total keys", node.getCount()));
        result.add(new AbstractMap.SimpleEntry<>(prefix + " total children", node.getChildrenCount()));

        node.getChildren()
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().getCount() - e1.getValue().getCount())
                .limit(n)
                .forEach(e -> result.add(getKeyAndCountOutput(e, prefix)));

        return result;
    }

    /**
     * Returns the full key and frequency of a child node.
     * @param _entry: Pair<Key Suffix, child TrieNode>.
     * @param prefix: prefix of the parent node.
     *
     * @return Pair<full key, frequency of the key>.
     */
    public Map.Entry<String, Integer> getKeyAndCountOutput(Map.Entry<String, TrieNode> _entry, String prefix) {
        return Map.entry(prefix.concat("/").concat(_entry.getKey()).concat("/*"), _entry.getValue().getCount());
    }

    /**
     * Returns the frequency of a node that represents the given key.
     * @param _prefix: key to be searched.
     *
     * @return an integer representing the frequency of the node.
     * @throws Exception: if the key is not found in the trie.
     */
    public Integer getCountForPrefix(String _prefix) throws Exception {
        final String prefix = _prefix.replaceFirst("/$", "");
        return traverseTrie(prefix).getCount();
    }

    /**
     * Traverse the trie from the root node to the node that represents the given path.
     * @param path: path to be traversed.
     *
     * @return TrieNode that represents the given path.
     * @throws Exception: if the path is not found in the trie.
     */
    private TrieNode traverseTrie(String path) throws Exception {
        StringTokenizer tokenizer = new StringTokenizer(path, "/");
        TrieNode current = root;
        while (tokenizer.hasMoreTokens()) {
            String nextKey = tokenizer.nextToken();
            if (!current.hasChild(nextKey)) {
                throw new Exception("Prefix not found");
            }
            current = current.getChild(nextKey);
        }
        return current;
    }
}
