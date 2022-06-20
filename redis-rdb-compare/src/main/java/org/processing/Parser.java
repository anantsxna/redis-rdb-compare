package org.processing;

import static java.lang.Thread.currentThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.threading.FixedNameableExecutorService;

/**
 * Parser class.
 * Processes the .rdb file and creates a list of all the keys.
 * Only handles the processing part of the parsing process.
 * The input, output files are part of the BotSession class that holds the Parser object.
 */
@Slf4j
@Builder
public final class Parser {

    @Builder.Default
    private final HashMap<String, String> parsePairs = new HashMap<>();

    @Builder.Default
    private final ExecutorService loggingExecutor = FixedNameableExecutorService
        .builder()
        .baseName("logger-in-parser-threads")
        .threadsNum(4)
        .build()
        .getExecutorService();

    /**
     * Method for thread that gathers the logs from the redis-rdb-tools python script.
     * Thread-safe method because logging is thread-safe and parameters are immutable.
     *
     * @param process:  the process which runs the script.
     * @param dumpFile: write-file fpr the process
     */
    private void watch(final Process process, final String dumpFile) {
        if (loggingExecutor.isShutdown()) {
            log.error("Logger thread is shutdown");
            return;
        }
        loggingExecutor.submit(() -> {
            log.info("Monitoring Process {}", process.toString());
            String line = null;
            try (
                BufferedReader input = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
                )
            ) {
                while ((line = input.readLine()) != null) {
                    log.info("PYPY3!: {} in File {}", line, dumpFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                log.info("{}} is closing...", currentThread().getName());
            }
        });
    }

    /**
     * Runnable for thread that gathers the errors from the redis-rdb-tools python script.
     * Thread-safe method becuase logging is thread-safe and parameters are immutable.
     *
     * @param process:  the process which runs the script
     * @param dumpFile: write-file for the process
     */
    private void watchErrors(final Process process, final String dumpFile) {
        if (loggingExecutor.isShutdown()) {
            log.error("Logger thread is shutdown");
            return;
        }
        loggingExecutor.submit(() -> {
            log.info("Monitoring Process {}", process.toString());
            String line = null;
            try (
                BufferedReader errors = new BufferedReader(
                    new InputStreamReader(process.getErrorStream())
                )
            ) {
                while ((line = errors.readLine()) != null) {
                    log.error("PYPY3 Error: {} in File {}", line, dumpFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                log.info("{}} is closing...", currentThread().getName());
            }
        });
    }

    /**
     * Adds the dump and key file to the list.
     *
     * @param dumpFile: the location of the dump file to parse, must be a .rdb file.
     * @param keysFile: the location of the file to store the keys, must be a .txt file.
     */
    public void addToParser(String dumpFile, String keysFile) {
        log.info("adding health, {}, {}", dumpFile, keysFile);
        parsePairs.put(dumpFile, keysFile);
    }

    /**
     * Parses the dumpFile and stores the keys in keysFile for each pair.
     * <p>
     * The keys will be stored in the same order as they appear in the dump file.
     */
    public void parse() {
        List<Process> parseProcesses = new ArrayList<>();
        parsePairs.forEach((dumpFile, keysFile) -> {
            String[] command = new String[] {
                "pypy3",
                "fast-parse.py",
                "--rdb=" + dumpFile,
                "--keys=" + keysFile,
                "--objspace-std-withsmalllong",
            };
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            log.info("Parsing dump file {} into keys file {}", dumpFile, keysFile);
            Process process = null;
            try {
                process = pb.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            watch(process, dumpFile);
            watchErrors(process, dumpFile);
            parseProcesses.add(process);
        });

        parseProcesses.forEach(process -> {
            int exitStatus;
            try {
                exitStatus = process.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (exitStatus != 0) {
                log.error("PYPY3: Process exited with status {}", exitStatus);
                throw new RuntimeException(
                    "ERROR: Process for parsing file exited with status " + exitStatus
                );
            } else {
                log.info("PYPY3: Process exited with status {}", exitStatus);
            }
        });
        loggingExecutor.shutdownNow();
    }

    /**
     * Clears the list of files to parse.
     */
    public void clear() {
        parsePairs.clear();
    }
}
