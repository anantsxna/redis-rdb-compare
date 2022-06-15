package org.processing;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Sorter {

    /**
     * Sorts the keys in the given keysFile using externalSortingInJava algorithm.
     * externalSortingInJava is implemented @ https://github.com/lemire/externalsortinginjava
     * @param keysFile: the file containing the keys to be sorted
     * @param sortedKeysFile: the file to write the sorted keys to
     * TODO: implement the function for phase-2. sorting not needed for phase-1.
     */
    static void sort(String keysFile, String sortedKeysFile) {
        log.info(
            "Sorting keys in file " + keysFile + " and saving result in file " + sortedKeysFile
        );
    }
}
