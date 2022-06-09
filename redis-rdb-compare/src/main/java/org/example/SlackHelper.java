package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processing.Parser;

public class SlackHelper {
    private static volatile String dumpA = "../dump-A.rdb";
    private static volatile  String dumpB = "../dump-B.rdb";
    private static volatile  String keysA = "../keys-A.txt";
    private static volatile  String keysB = "../keys-B.txt";

    private static final Logger logger = LogManager.getLogger(SlackHelper.class);

    private enum ParsingStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }
    private static volatile ParsingStatus parsingStatus = ParsingStatus.NOT_STARTED;

    private static void parseAux() {
        new Thread(() -> {
            Parser.parse();
            parsingStatus = ParsingStatus.COMPLETED;
            //TODO: Find a way to send a message to the user that the parsing is completed
        }).start();
    }
    public static String parseViaSlack() {
        if(parsingStatus == ParsingStatus.IN_PROGRESS) {
            System.out.println("Parsing already in progress. Please wait.");
            return "Parsing already in progress. Please wait.";
        }

        if(parsingStatus == ParsingStatus.COMPLETED) {
            System.out.println("Parsing already completed for some files.");
            return "Parsing already completed for some files.";
            //TODO: ask user for intent, reset parsingStatus to NOT_STARTED and skip return
        }

        //TODO: ask user for input regarding file location
        //TODO: download files from s3 link and save to local directory
        Parser.clear();
        Parser.addToParser(dumpA, keysA);
        Parser.addToParser(dumpB, keysB);
        parsingStatus = ParsingStatus.IN_PROGRESS;
        parseAux();
        return "Parsing has started. Please wait. Use /parse command to check status.";
    }



}
