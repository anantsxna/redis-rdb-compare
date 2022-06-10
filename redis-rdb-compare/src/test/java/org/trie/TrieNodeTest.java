package org.trie;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TrieNodeTest {

    @Test
    void initCount() {
        System.out.println("Running initCount test...");
        TrieNode node = new TrieNode();
        assertEquals(node.getCount(), 0);
    }
}
