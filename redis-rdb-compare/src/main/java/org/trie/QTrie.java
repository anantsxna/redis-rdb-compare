package org.trie;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Trie class.
 */
@Slf4j
@Builder
public final class QTrie {

    @Builder.Default
    private final TrieNode root = TrieNode.builder().build();

    @NonNull
    private final String keysFile;

    @NotNull
    public String getKeysFile() {
        return keysFile;
    }

    /**
     * Reads keys from file and inserts them into trie.
     */
    public void takeInput() {
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
                if (i % 100000 == 0) {
                    log.info("Read " + i + " keys.");
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
    private void insertKey(String dbKey) {
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
            } else break;
        }
    }

    /**
     * Returns the 'n' maximum count child nodes of a node that represents the given prefix.
     *
     * @param _prefix: prefix to be searched.
     * @param n: number of child nodes to be returned.
     * @return List of (n+2) pairs, where:
     * the first pair has the total number of keys with the prefix '_prefix'.
     * the second pair has the total number of child nodes of the node that represents the prefix '_prefix'.
     * each of the next n pairs contains the child keys in decreasing order of their count.
     * If the number of child nodes is less than n, List has less than (n+2) pairs.
     */
    public List<Map.Entry<String, Integer>> topNKeyWithPrefix(String _prefix, int n)
        throws Exception {
        final String prefix = _prefix.replaceFirst("/$", "");

        List<Map.Entry<String, Integer>> result = new ArrayList<>();
        TrieNode node = traverseTrie(prefix);

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
            prefix.concat("/").concat(_entry.getKey()).concat("/*"),
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
    public Integer getCountForPrefix(String _prefix) throws Exception {
        final String prefix = _prefix.replaceFirst("/$", "");
        return traverseTrie(prefix).getCount();
    }

    /**
     * Traverse the trie from the root node to the node that represents the given path.
     *
     * @param path: path to be traversed.
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
