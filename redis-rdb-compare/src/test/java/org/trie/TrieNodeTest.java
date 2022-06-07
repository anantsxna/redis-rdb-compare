package org.trie;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrieNodeTest {
    @Test
    void initCount() {
        System.out.println("Running initCount test...");
        TrieNode node = new TrieNode();
        assertEquals(node.getCount(), 0);
    }
}