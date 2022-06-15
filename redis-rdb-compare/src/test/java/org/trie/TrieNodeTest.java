package org.trie;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrieNodeTest {

    @Test
    void initCount() {
        System.out.println("Running initCount test...");
        TrieNode node = TrieNode.builder().build();
        assertEquals(node.getCount(), 0);
    }
}
