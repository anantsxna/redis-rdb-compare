package org.processing;

import static org.example.Main.props;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3Object;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.threading.FixedNameableExecutorService;

/**
 * Downloader class.
 * Processes the s3 links and downloads the .rdb files.
 * Only handles the processing part of the downloading process.
 * The input links, output files are part of the BotSession class that holds the Downloader object.
 */
@Slf4j
@Builder
public class Downloader {

    private static final long DOWNLOAD_TIMEOUT_SECONDS = Integer.parseInt(
        props.getProperty("DOWNLOAD_TIMEOUT_SECONDS")
    );

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
     * @param s3Link:   the s3 URL to the .rdb file.
     * @param dumpFile: the output file to write the .rdb file to.
     */
    public void addToDownloader(String s3Link, String dumpFile) {
        downloadPairs.put(s3Link, dumpFile);
    }

    @Builder
    private record PublicURLDownloaderTask(URL s3url, String dumpFile) implements Callable<String> {
        @Override
        public String call() {
            log.info("Public URL Downloading {} to {}", s3url.toString(), dumpFile);
            ReadableByteChannel readableByteChannel = null;
            try {
                readableByteChannel = Channels.newChannel(s3url.openStream());
            } catch (IOException e) {
                log.error("Error in ReadableByteChannel");
                log.error(e.getMessage());
                return props.getProperty("DOWNLOAD_ERROR");
            }
            try (FileOutputStream fileOutputStream = new FileOutputStream(dumpFile)) {
                try {
                    FileChannel fileChannel = fileOutputStream.getChannel();
                    fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                } catch (IOException e) {
                    log.error("Error in transferFrom");
                    return props.getProperty("DOWNLOAD_ERROR");
                }
            } catch (FileNotFoundException e) {
                log.error("Error in FileNotFound");
                log.error(e.getMessage());
                return props.getProperty("DOWNLOAD_ERROR");
            } catch (SecurityException e) {
                log.error("Error in Security");
                log.error(e.getMessage());
                return props.getProperty("DOWNLOAD_ERROR");
            } catch (IOException e) {
                log.error("Error in FileOutputStream");
                log.error(e.getMessage());
                return props.getProperty("DOWNLOAD_ERROR");
            }
            return props.getProperty("DOWNLOAD_SUCCESS");
        }
    }

    @Builder
    private record S3DownloaderTask(String s3url, String dumpFile) implements Callable<String> {
        @Override
        public String call() throws IOException {
            log.info("S3 URL Downloading {} to {}", s3url, dumpFile);
            Regions clientRegion = Regions.DEFAULT_REGION;
            String url = s3url;
            //            log.info("{} {}", s3url, s3url.replaceFirst("s3://", ""));
            String tempTokens[] = s3url.replaceFirst("s3://", "").split("/", 2);
            String bucketName = tempTokens[0];
            String key = tempTokens[1];

            S3Object fullObject = null, objectPortion = null, headerOverrideObject = null;
            try {
                AmazonS3 s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(clientRegion)
                    .withCredentials(new InstanceProfileCredentialsProvider())
                    .build();

                //                AmazonS3 s3Client = AmazonS3ClientBuilder
                //                    .standard()
                //                    .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                //                    .build();

                // Get an entire object, overriding the specified response headers, and print the object's content.
                ResponseHeaderOverrides headerOverrides = new ResponseHeaderOverrides()
                    .withCacheControl("No-cache")
                    .withContentDisposition("attachment; filename=" + dumpFile);
                GetObjectRequest getObjectRequestHeaderOverride = new GetObjectRequest(
                    bucketName,
                    key
                )
                    .withResponseHeaders(headerOverrides);
                headerOverrideObject = s3Client.getObject(getObjectRequestHeaderOverride);
                //                displayTextInputStream(headerOverrideObject.getObjectContent());
            } catch (AmazonServiceException e) {
                // The call was transmitted successfully, but Amazon S3 couldn't process
                // it, so it returned an error response.
                e.printStackTrace();
                log.info(e.getMessage());
                return props.getProperty("DOWNLOAD_ERROR");
            } catch (SdkClientException e) {
                // Amazon S3 couldn't be contacted for a response, or the client
                // couldn't parse the response from Amazon S3.
                e.printStackTrace();
                log.info(e.getMessage());
                return props.getProperty("DOWNLOAD_ERROR");
            } finally {
                // To ensure that the network connection doesn't remain open, close any open input streams.
                if (fullObject != null) {
                    fullObject.close();
                }
                if (objectPortion != null) {
                    objectPortion.close();
                }
                if (headerOverrideObject != null) {
                    headerOverrideObject.close();
                }
            }
            return props.getProperty("DOWNLOAD_SUCCESS");
        }
    }

    /**
     * Downloads the .rdb files.
     */
    public boolean download(boolean isS3URL) throws InterruptedException {
        AtomicBoolean downloadingSuccess = new AtomicBoolean(true);
        assert (!downloadingExecutor.isShutdown());
        log.info("download() called...");

        if (isS3URL) {
            List<S3DownloaderTask> taskList = new ArrayList<>();
            downloadPairs.forEach((s3url, dumpFile) -> {
                taskList.add(S3DownloaderTask.builder().dumpFile(dumpFile).s3url(s3url).build());
                try {
                    List<Future<String>> futures = downloadingExecutor.invokeAll(
                        taskList,
                        DOWNLOAD_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                    );
                    futures.forEach(future -> {
                        try {
                            String result = future.get();
                            if (!result.equals(props.getProperty("DOWNLOAD_SUCCESS"))) {
                                downloadingSuccess.set(false);
                            }
                        } catch (Exception e) {
                            log.error("Error in download()");
                            log.error(e.getMessage());
                            downloadingSuccess.set(false);
                        }
                    });
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            List<PublicURLDownloaderTask> taskList = new ArrayList<>();
            downloadPairs.forEach((s3url, dumpFile) -> {
                try {
                    taskList.add(
                        PublicURLDownloaderTask
                            .builder()
                            .dumpFile(dumpFile)
                            .s3url(new URL(s3url))
                            .build()
                    );
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                try {
                    List<Future<String>> futures = downloadingExecutor.invokeAll(
                        taskList,
                        DOWNLOAD_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                    );
                    futures.forEach(future -> {
                        try {
                            String result = future.get();
                            if (!result.equals(props.getProperty("DOWNLOAD_SUCCESS"))) {
                                downloadingSuccess.set(false);
                            }
                        } catch (Exception e) {
                            log.error("Error in download()");
                            log.error(e.getMessage());
                            downloadingSuccess.set(false);
                        }
                    });
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        downloadingExecutor.shutdownNow();
        log.info(downloadingSuccess.get() ? "Downloading successful" : "Downloading failed");
        return downloadingSuccess.get();
    }
}
