package org.querying;

import static org.example.Channel.getChannel;

import org.example.Channel;

public class countQuery extends Query {

    private final String key;

    public countQuery(String text, String channelId) {
        super(QueryType.GET_COUNT, channelId);
        key = text;
    }

    public void execute() {
        System.out.println("executing count query...!");
        try {
            Channel channel = getChannel(getChannelId());
            int countInA = channel.trieA.getCountForPrefix(key);
            int countInB = channel.trieB.getCountForPrefix(key);
            result
                .append("Total keys with prefix *")
                .append(key)
                .append("*: ")
                .append("in first database: ")
                .append(countInA)
                .append(", ")
                .append("in second database: ")
                .append(countInB)
                .append("\n");
        } catch (Exception e) {
            setExitCode(1);
            throw new RuntimeException(e);
        }
        setExitCode(0);
    }

    public String result() {
        if (getExitCode() == 0) {
            return result.toString();
        } else if (getExitCode() == -1) {
            return "Error: Query could not execute";
        } else {
            return "Error: Query failed";
        }
    }
}
