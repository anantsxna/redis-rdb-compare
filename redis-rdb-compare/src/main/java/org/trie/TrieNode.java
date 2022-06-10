package org.trie;

import java.util.HashMap;

/**
 * Trie Node class.
 */
public final class TrieNode {

    private final HashMap<String, TrieNode> children; // children of the node
    private Integer count; // count of strings that go through this node

    public TrieNode() {
        children = new HashMap<>();
        count = 0;
    }

    public Integer getCount() {
        return count;
    }

    public HashMap<String, TrieNode> getChildren() {
        return children;
    }

    public Integer getChildrenCount() {
        return children.size();
    }

    public void addCount() {
        count += 1;
    }

    public boolean hasChild(String childName) {
        return children.containsKey(childName);
    }

    public TrieNode getChild(String childName) {
        return children.get(childName);
    }

    public void addChild(String childName) {
        children.put(childName, new TrieNode());
    }
}
