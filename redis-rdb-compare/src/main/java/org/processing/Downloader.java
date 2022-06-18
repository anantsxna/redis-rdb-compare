package org.processing;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final HashMap<String, String> downloadPairs = new HashMap<>();

    @Builder.Default
    private final ExecutorService downloadingExecutor = FixedNameableExecutorService
        .builder()
        .baseName("downloader-threads")
        .threadsNum(2)
        .build()
        .getExecutorService();

    /**
     * Adds the s3 link and the output file to the list.
     *
     * @param s3Link:   the s3 link to the .rdb file.
     * @param dumpFile: the output file to write the .rdb file to.
     */
    public void addToDownloader(String s3Link, String dumpFile) {
        downloadPairs.put(s3Link, dumpFile);
    }

    /**
     * Downloads the .rdb files.
     */
    public boolean download() throws InterruptedException {
        AtomicBoolean downloadingSuccess = new AtomicBoolean(true);
        assert (!downloadingExecutor.isShutdown());
        log.info("download() called...");
        downloadPairs.forEach((s3Link, dumpFile) -> {
            downloadingExecutor.submit(() -> {
                log.info("Downloading {} to {}", s3Link, dumpFile);
                //  TODO: S3Downloader.download(s3Link, dumpFile);
                URL url = null;
                try {
                    url = new URL(s3Link);
                } catch (MalformedURLException e) {
                    log.error("Malformed URL: {}", s3Link);
                    throw new RuntimeException(e);
                }
                try (
                    ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                    FileOutputStream fileOutputStream = new FileOutputStream(dumpFile)
                ) {
                    FileChannel fileChannel = fileOutputStream.getChannel();
                    try {
                        fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                    } catch (IOException e) {
                        log.error("Error in transferFrom");
                        downloadingSuccess.set(false);
                    }
                } catch (FileNotFoundException e) {
                    log.error("Error in FileNotFound");
                    downloadingSuccess.set(false);
                } catch (SecurityException e) {
                    log.error("Error in Security");
                    downloadingSuccess.set(false);
                } catch (IOException e) {
                    log.error("Error in FileOutputStream");
                    downloadingSuccess.set(false);
                }
            });
        });
        downloadingExecutor.shutdown();
        log.info(downloadingSuccess.get() ? "Downloading successful" : "Downloading failed");
        boolean awaitTerm = downloadingExecutor.awaitTermination(
            Long.MAX_VALUE, //TODO: ask sir for a value
            TimeUnit.SECONDS
        );
        return downloadingSuccess.get() && awaitTerm;
    }
}