package org.querying;

import static org.example.Channel.getChannel;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.example.Channel;

@SuperBuilder
public class countQuery extends Query {

    @NonNull
    private final String key;

    public void execute() {
        System.out.println("executing count query...!");
        try {
            Channel channel = getChannel(getChannelId());
            int countInA = channel.getTrieA().getCountForPrefix(key);
            int countInB = channel.getTrieB().getCountForPrefix(key);
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
            result.append("The key does not exist in the database.");
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
