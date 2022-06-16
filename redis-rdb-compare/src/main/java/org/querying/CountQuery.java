package org.querying;

import static org.example.Channel.getChannel;

import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.core.util.Assert;
import org.example.Channel;

/**
 * "/getcount [prefixKey]" query.
 */
@Slf4j
@SuperBuilder
public class CountQuery extends Query {

    private final String key;

    @Override
    public void execute() {
        try {
            Channel channel = getChannel(getChannelId());
            long startTime = System.currentTimeMillis();
            int countInA = channel.getTrieA().getCountForPrefix(key);
            int countInB = channel.getTrieB().getCountForPrefix(key);
            long endTime = System.currentTimeMillis();
            result
                .append("Total keys with prefix *")
                .append(key)
                .append("*: ")
                .append("in first database: ")
                .append(countInA)
                .append(", ")
                .append("in second database: ")
                .append(countInB)
                .append("\n")
                .append("query time: ")
                .append(endTime - startTime)
                .append(" ms\n");
            log.info("Count query for key: {} in channel: {}", key, getChannelId());
        } catch (Exception e) {
            result.append("The key does not exist in the database.");
        }
        setExitCode(0);
    }

    @Override
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
