package org.trie;

import static org.junit.jupiter.api.Assertions.*;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class TrieNodeTest {

    @Test
    void initCount() {
        log.info("Running initCount test...");
        TrieNode node = TrieNode.builder().build();
        assertEquals(node.getCount(), 0);
    }

    @Test
    void addCount() {
        log.info("Running addCount test...");
        TrieNode node = TrieNode.builder().build();
        node.addCount();
        assertEquals(node.getCount(), 1);
    }

    @Test
    void hasChild() {
        log.info("Running hasChild test...");
        TrieNode node = TrieNode.builder().build();
        assertFalse(node.hasChild("a"));
    }

    @Test
    void getChild() {
        log.info("Running getChild test...");
        TrieNode node = TrieNode.builder().build();
        assertNull(node.getChild("a"));
    }

    @Test
    void addChild() {
        log.info("Running addChild test...");
        TrieNode node = TrieNode.builder().build();
        node.addChild("a");
        assertTrue(node.hasChild("a"));
    }

    @Test
    void getChildrenCount() {
        log.info("Running getChildrenCount test...");
        TrieNode node = TrieNode.builder().build();
        assertEquals(node.getChildrenCount(), 0);
    }

    @Test
    void getChildren() {
        log.info("Running getChildren test...");
        TrieNode node = TrieNode.builder().build();
        assertNotNull(node.getChildren());
    }
}
