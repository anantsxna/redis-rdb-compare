package org.querying;

import static org.example.BotSession.getBotSession;

import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.example.BotSession;

/**
 * "/getcount [prefixKey]" query.
 */
@Slf4j
@SuperBuilder
public class CountQuery extends Query {

    private final String key;

    @Override
    public void execute() {
        try (BotSession botSession = getBotSession(getRequestId())) {
            try {
                result.append(">In session ").append(getRequestId()).append("\n");
                long startTime = System.currentTimeMillis();
                int countInA = botSession.getTrieA().getCountForPrefix(key);
                int countInB = botSession.getTrieB().getCountForPrefix(key);
                long endTime = System.currentTimeMillis();
                result
                    .append("Total keys with prefix *")
                    .append(key)
                    .append("*: \n")
                    .append(">in first database: *")
                    .append(countInA)
                    .append("*, ")
                    .append("in second database: *")
                    .append(countInB)
                    .append("*\n")
                    .append("`query time: ")
                    .append(endTime - startTime)
                    .append(" ms`\n");
                log.info("Count query for key: {} in botSession: {}", key, getRequestId());
            } catch (Exception e) {
                result.append(">The key does not exist in the database.");
            }
            setExitCode(0);
        } catch (Exception e) {
            result.append(INVALID_REQUEST_ID);
            setExitCode(0);
        }
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
