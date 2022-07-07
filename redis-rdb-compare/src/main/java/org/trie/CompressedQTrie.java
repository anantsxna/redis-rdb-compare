package org.trie;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.Main;

/**
 * Trie class.
 */
@Slf4j
@Builder
public final class CompressedQTrie {

    @Builder.Default
    @Getter
    private final CompressedTrieNode root = CompressedTrieNode.builder().myPath("").build();

    @Builder.Default
    private final Properties props = Main.props;

    @NonNull
    private final QTrie trie;

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
     * Returns the count of a node that represents the given key.
     *
     * @param prefix: key to be searched.
     * @return an integer representing the count of the node.
     * @throws Exception: if the key is not found in the trie.
     */
    public Integer getCountForPrefix(String prefix) {
        //        final String prefix = _prefix.replaceFirst(DELIMITER + "$", "");
        //        log.info("Getting count for the prefix: {}", prefix);
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
    public CompressedTrieNode traverseTrie(String path) {
        final CompressedTrieNode[] current = { root };
        final String[] tempPath = { path };
        while (tempPath[0].length() > 0) {
            Boolean flag = (false);
            //            if (path.equals("ACCOUNT::")) {
            //                log.info("Remaining length: {}", tempPath[0]);
            //            }

            for (Map.Entry<String, CompressedTrieNode> entry : current[0].getChildren()
                .entrySet()) {
                String key = entry.getKey();
                CompressedTrieNode childNode = entry.getValue();
                //                if (path.equals("ACCOUNT::")) {
                //                    log.info("Key here: {}", key);
                //                }
                if (tempPath[0].startsWith(key)) {
                    //                    if (path.equals("ACCOUNT::")) {
                    //                        log.info("Found key: {}", key);
                    //                    }
                    tempPath[0] = tempPath[0].substring(key.length());
                    current[0] = childNode;
                    flag = (true);
                    break;
                }
            }

            if (!flag) {
                //                if (path.equals("ACCOUNT::")) {
                //                    log.info("Key not found: {}", tempPath[0]);
                //                }
                return null;
            }
        }
        return current[0];
    }

    public void takeInput() {
        matchNodes(root, trie.getRoot(), null, "");
    }

    private void matchNodes(
        CompressedTrieNode node,
        TrieNode copy,
        CompressedTrieNode parent,
        String start
    ) {
        node.setCount(copy.getCount());
        if (node == root) {
            node.setMyPath("");
            node.setCount(copy.getCount());
        } else {
            StringBuilder joinedString = new StringBuilder();
            joinedString.append(start);
            while (copy.getChildren().size() == 1) {
                Map.Entry<Character, TrieNode> soleChild = copy
                    .getChildren()
                    .entrySet()
                    .iterator()
                    .next();
                if (Objects.equals(copy.getCount(), soleChild.getValue().getCount())) {
                    joinedString.append(soleChild.getKey());
                    copy = soleChild.getValue();
                } else {
                    break;
                }
            }
            node.setMyPath(parent.getMyPath() + joinedString.toString());
            node.setCount(copy.getCount());
            node.setParentChildKeyDiff(joinedString.length());
            log.info("Path: {}, Count: {}", node.getMyPath(), node.getCount());
            parent.getChildren().put(joinedString.toString(), node);
        }

        copy
            .getChildren()
            .forEach((key, childNode) -> {
                CompressedTrieNode child = CompressedTrieNode
                    .builder()
                    .myPath(node.getMyPath() + key)
                    .build();
                matchNodes(child, childNode, node, key + "");
            });
    }

    //    private void matchNodes(CompressedTrieNode node, TrieNode copy, CompressedTrieNode parent, String parentRef) {
    //        log.info("Helloooo");
    //        if (node != root && copy.getChildren().size() == 1 && Objects.equals(copy.getCount(), copy.getChildren().entrySet().iterator().next().getValue().getCount())) {
    //            log.info("tata {}, {}, {}, {}", node.getMyPath(), copy.getChildren().size(), copy.getCount(), copy.getChildren().entrySet().iterator().next().getValue().getCount());
    //
    //            Map.Entry<Character, TrieNode> soleRelation = copy.getChildren().entrySet().iterator().next();
    //            TrieNode soleChild = soleRelation.getValue();
    //            Character relation = soleRelation.getKey();
    //
    //            //count will remain the same
    //            matchNodes(node, soleChild, parent, parentRef + relation);
    //        } else {
    //            log.info("in else");
    ////            if (parent.getMyPath().startsWith("LIN") && parent.getMyPath().length() < 10)
    //            log.info("hello {}, {}, {} {}", node.getMyPath(), copy.getChildren().size(), copy.getCount(), copy.getChildren().entrySet().iterator().next().getValue().getCount());
    //            node.setCount(copy.getCount());
    //            if (node != root) {
    //                parent.getChildren().put(parentRef, node);
    //                node.setMyPath(parent.getMyPath() + parentRef);
    //            }
    //            copy.getChildren().forEach((key, child) -> {
    //                log.info("bye {}, {}, {}", key, child.getChildren().size(), child.getCount());
    //                CompressedTrieNode childNode = CompressedTrieNode.builder().build();
    //                matchNodes(childNode, child, node, key + "");
    //            });
    //        }
    //    }

    public void show() {
        showRelations(root);
    }

    public void showRelations(CompressedTrieNode node) {
        node
            .getChildren()
            .forEach((key, child) -> {
                //            log.info("{}, {} --> {}, {}", node.getMyPath(), node.getCount(), child.getMyPath(), child.getCount());
                //            assert node.getMyPath() + key == child.getMyPath();
            });
        node
            .getChildren()
            .forEach((key, child) -> {
                showRelations(child);
            });
    }
}
