package org.trie;

import lombok.Builder;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Trie Node class.
 */
@Builder
public final class CompressedTrieNode {

    @NonNull
    @Builder.Default
    private final HashMap<String, CompressedTrieNode> children = new HashMap<>(); // children of the node

    @Builder.Default
    @Setter
    private Integer count = 0; // count of strings that go through this node

    public Integer getCount() {
        return count;
    }

    @NotNull
    public HashMap<String, CompressedTrieNode> getChildren() {
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

    public CompressedTrieNode getChild(char childName) {
        return children.get(childName);
    }

    public void addChild(String childName) {
        children.put(childName, CompressedTrieNode.builder().build());
    }
}
