package org.processing;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.threading.FixedNameableExecutorService;
import org.trie.QTrie;

/**
 * Trie Maker class.
 * Processes the keys class and makes the tries.
 * Only handles the processing part of the trie making process.
 * The input, output files are part of the BotSession class that holds the TrieMaker object.
 */
@Slf4j
@Builder
public class TrieMaker {

    @Builder.Default
    public static final int TIMEOUT_SECONDS = 300;

    @Builder.Default
    private final HashMap<String, QTrie> keysTriesPairs = new HashMap<>();

    @Builder.Default
    private final ExecutorService trieInsertionExecutorService = FixedNameableExecutorService
        .builder()
        .baseName("trie-initializer-threads")
        .threadsNum(2)
        .build()
        .getExecutorService();

    /**
     * Adds the key files and Qtrie object to the list.
     *
     * @param keysFile: the location of the file to get the keys, must be a .txt file.
     * @param trie:     the trie to be made.
     */
    public void addToTrieMaker(String keysFile, QTrie trie) {
        keysTriesPairs.put(keysFile, trie);
    }

    /**
     * Initialize the tries.
     */
    public boolean makeTries() throws InterruptedException {
        keysTriesPairs.forEach((keysFile, trie) -> {
            trieInsertionExecutorService.submit(() -> {
                log.info("Inserting keys from {}", keysFile);
                trie.takeInput();
            });
        });
        trieInsertionExecutorService.shutdown();
        return trieInsertionExecutorService.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
