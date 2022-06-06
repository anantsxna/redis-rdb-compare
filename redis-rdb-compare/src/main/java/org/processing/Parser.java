package org.processing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * Parser class.
 * Processes the .rdb file and creates a list of all the keys.
 */
public final class Parser {
    private static final Logger logger = LogManager.getLogger(Parser.class);

    /**
     * Runnable for thread that gathers the logs from the redis-rdb-tools python script.
     * @param process: the process which runs the script.
     *
     */
    private static void watch(final Process process, final String dumpFile) {
        new Thread(() -> {
            logger.info("Monitoring Process {}", process.toString());
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            try {
                while ((line = input.readLine()) != null) {
                    logger.info("PYPY3: {} in File {}", line, dumpFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Parses the given dumpFile and stored the keys in keysFile.
     *
     * @param dumpFile: the location of the dump file to parse, must be a .rdb file.
     * @param keysFile: the location of the file to store the keys, must be a .txt file.
     * The keys will be stored in the same order as they appear in the dump file.
     *
     */
    public static void parse(String dumpFile, String keysFile) {
        logger.info("Parsing dump file {} into keys file {}", dumpFile, keysFile);
        try {
            String[] command = new String[] {
                    "pypy3",
                    "../redis-rdb-tools/fast-parse.py",
                    "--rdb=" + dumpFile,
                    "--keys=" + keysFile,
                    "--objspace-std-withsmalllong",
            };
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            watch(process, dumpFile);
            Integer exitStatus = process.waitFor();
            if (exitStatus != 0) {
                logger.error("PYPY3: Process exited with status {}", exitStatus);
                throw new RuntimeException("ERROR: Process for parsing file" + dumpFile + "exited with status " + exitStatus);
            }
            else {
                logger.info("PYPY3: Process exited with status {}", exitStatus);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
