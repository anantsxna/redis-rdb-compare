package org.trie;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
