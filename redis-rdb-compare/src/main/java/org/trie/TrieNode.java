package org.trie;

import java.util.HashMap;
import lombok.Builder;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * Trie Node class.
 */
@Builder
public final class TrieNode {

    @NonNull
    @Builder.Default
    private final HashMap<Character, TrieNode> children = new HashMap<>(); // children of the node

    @Builder.Default
    private Integer count = 0; // count of strings that go through this node

    public Integer getCount() {
        return count;
    }

    @NotNull
    public HashMap<Character, TrieNode> getChildren() {
        return children;
    }

    public Integer getChildrenCount() {
        return children.size();
    }

    public void addCount() {
        count += 1;
    }

    public boolean hasChild(char childName) {
        return children.containsKey(childName);
    }

    public TrieNode getChild(char childName) {
        return children.get(childName);
    }

    public void addChild(char childName) {
        children.put(childName, TrieNode.builder().build());
    }
}
