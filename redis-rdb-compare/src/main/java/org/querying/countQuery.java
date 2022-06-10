package org.querying;

import org.example.SlackHelper;

public class countQuery extends Query {

    private final String key;
    private int countInA = 0, countInB = 0;

    public countQuery(String text) {
        super(QueryType.GET_COUNT);
        key = text;
    }

    public void execute() {
        System.out.println("executing count query...!");
        try {
            countInA = SlackHelper.trieA.getCountForPrefix(key);
            countInB = SlackHelper.trieB.getCountForPrefix(key);
        } catch (Exception e) {
            setExitCode(1);
            throw new RuntimeException(e);
        }
        setExitCode(0);
    }

    public String result() {
        if (getExitCode() == 0) {
            return (
                "Count for " +
                key +
                "/* in first database: " +
                countInA +
                " and in second database: " +
                countInB
            );
        } else if (getExitCode() == -1) {
            return "Error: Query could not execute";
        } else {
            return "Error: Query failed";
        }
    }
}
