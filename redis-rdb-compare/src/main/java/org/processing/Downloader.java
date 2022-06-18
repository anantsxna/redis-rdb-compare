package org.processing;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.threading.FixedNameableExecutorService;

/**
 * Downloader class.
 * Processes the s3 links and downloads the .rdb files.
 * Only handles the processing part of the downloading process.
 * The input links, output files are part of the Channel class that holds the Downloader object.
 */
@Slf4j
@Builder
public class Downloader {

    @Builder.Default
    private static final HashMap<String, String> downloadPairs = new HashMap<>();

    @Builder.Default
    private static final ExecutorService downloadingExecutor = FixedNameableExecutorService
        .builder()
        .baseName("downloader-threads")
        .threadsNum(2)
        .build()
        .getExecutorService();

    /**
     * Adds the key files and Qtrie object to the list.
     * @param keysFile: the location of the file to get the keys, must be a .txt file.
     * @param trie: the trie to be made.
     *
     */
    public void addToDownloader(String s3Link, String keysFile) {
        downloadPairs.put(s3Link, keysFile);
    }
}
