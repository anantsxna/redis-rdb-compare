package org.processing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Sorter {

    private static final Logger logger = LogManager.getLogger(Sorter.class);

    /**
     * Sorts the keys in the given keysFile using externalSortingInJava algorithm.
     * externalSortingInJava is implemented @ https://github.com/lemire/externalsortinginjava
     * @param keysFile: the file containing the keys to be sorted
     * @param sortedKeysFile: the file to write the sorted keys to
     * TODO: implement the function for phase-2. sorting not needed for phase-1.
     */
    static void sort(String keysFile, String sortedKeysFile) {
        logger.info(
            "Sorting keys in file " + keysFile + " and saving result in file " + sortedKeysFile
        );
    }
}
