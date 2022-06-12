package org.trie;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TrieNodeTest {

    @Test
    void initCount() {
        System.out.println("Running initCount test...");
        TrieNode node = TrieNode.builder().build();
        assertEquals(node.getCount(), 0);
    }
}
