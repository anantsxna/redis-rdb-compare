package org.trie;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.Main;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Trie class.
 */
@Slf4j
@Builder
public final class CompressedQTrie {

    @Builder.Default
    @Getter
    private final CompressedTrieNode root = CompressedTrieNode.builder().build();

    @Builder.Default
    private final Properties props = Main.props;

    @NonNull
    private final QTrie trie;

    /**
     * Returns the 'n' maximum count child nodes of a node that represents the given prefix.
     *
     * @param prefix: prefix to be searched.
     * @param n:      number of child nodes to be returned.
     * @return List of (n+2) pairs, where:
     * the first pair has the total number of keys with the prefix '_prefix'.
     * the second pair has the total number of child nodes of the node that represents the prefix '_prefix'.
     * each of the next n pairs contains the child keys in decreasing order of their count.
     * If the number of child nodes is less than n, List has less than (n+2) pairs.
     */
    public List<Map.Entry<String, Integer>> topNKeyWithPrefix(String prefix, int n)
            throws Exception {
        //        final String prefix = _prefix.replaceFirst(DELIMITER + "$", "");

        List<Map.Entry<String, Integer>> result = new ArrayList<>();
        CompressedTrieNode node = traverseTrie(prefix);
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
            Set<String> children = new HashSet<>();
            root.getChildren().keySet().forEach(c -> children.add(c + ""));
            return children;
        }
        CompressedTrieNode node = traverseTrie(prefix);
        if (node == null) {
            return new HashSet<>();
        }
        Set<String> children = new HashSet<>();
        node
                .getChildren()
                .keySet()
                .forEach(child -> {
                    children.add(prefix + child);
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
            Map.Entry<String, CompressedTrieNode> _entry,
            String prefix
    ) {
        return Map.entry(prefix.concat(_entry.getKey()), _entry.getValue().getCount());
    }

    /**
     * Returns the count of a node that represents the given key.
     *
     * @param prefix: key to be searched.
     * @return an integer representing the count of the node.
     * @throws Exception: if the key is not found in the trie.
     */
    public Integer getCountForPrefix(String prefix) {
        //        final String prefix = _prefix.replaceFirst(DELIMITER + "$", "");
        log.info("Getting count for prefix: {}", prefix);
        CompressedTrieNode node = traverseTrie(prefix);
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
    private CompressedTrieNode traverseTrie(String path) {
        final CompressedTrieNode[] current = {root};
        final String[] tempPath = {path};
        while (tempPath[0].length() > 0) {
            AtomicBoolean flag = new AtomicBoolean(false);
            current[0].getChildren().forEach((key, childNode) -> {
                if (tempPath[0].startsWith(key)) {
                    tempPath[0] = tempPath[0].substring(key.length());
                    current[0] = childNode;
                    flag.set(true);
                }
            });
            if (!flag.get()) {
                return null;
            }
        }
        return current[0];
    }

    public void takeInput() {
        matchNodes(root, trie.getRoot(), null, "");
    }

    private void matchNodes(CompressedTrieNode node, TrieNode copy, CompressedTrieNode parent, String parentRef) {
        node.setCount(copy.getCount());
        log.info("parentref = {}", parentRef);
        if (node != root && copy.getChildren().size() == 1 && Objects.equals(copy.getChildren().entrySet().iterator().next().getValue().getCount(), copy.getCount())) {
            parent.getChildren().remove(parentRef);
            parent.getChildren().put(parentRef + copy.getChildren().entrySet().iterator().next().getKey(), node);
            matchNodes(node, copy.getChildren().entrySet().iterator().next().getValue(), parent, parentRef + copy.getChildren().entrySet().iterator().next().getKey());
        } else {
            copy.getChildren().forEach((key, child) -> {
                CompressedTrieNode childNode = node.getChildren().get(key);
                if (childNode == null) {
                    childNode = CompressedTrieNode.builder().build();
                    node.getChildren().put(key + "", childNode);
                }
                matchNodes(childNode, child, node, key + "");
            });
        }
    }

}
