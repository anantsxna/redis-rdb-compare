package org.querying;

import static org.example.BotSession.getBotSession;
import static org.example.Main.props;

import java.util.*;

import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.example.BotSession;

/**
 * "/getcount [prefixKey]" query.
 */
@Slf4j
@SuperBuilder
public class CountQuery extends Query {

    @NonNull
    private String key;

    @NonNull
    @Builder.Default
    private Integer head = Integer.parseInt(props.getProperty("DEFAULT_HEAD"));

    @Override
    public void execute() {
        try {
            BotSession botSession = getBotSession(getRequestId());

            result.append(">In session ").append(getRequestId()).append("\n\n");
            long startTime = System.currentTimeMillis();

            assert botSession != null;
            if (key.equals("!root")) {
                key = "";
            }
            Set<String> setA = botSession.getTrieC().getChildren(key);
            Set<String> setB = botSession.getTrieD().getChildren(key);

            Set<String> setCombine = new HashSet<>();
            setCombine.addAll(setA);
            setCombine.addAll(setB);

            List<Map.Entry<Integer, String>> sortedResult = new ArrayList<>();
            for (String parentKey : setCombine) {
                int countInA = botSession.getTrieC().getCountForPrefix(parentKey);
                int countInB = botSession.getTrieD().getCountForPrefix(parentKey);
                sortedResult.add(new AbstractMap.SimpleEntry<>((countInA - countInB), parentKey));
            }

            Collections.sort(sortedResult, Comparator.comparing(p -> -Math.abs(p.getKey())));

            for (int i = 0; i < Math.min(head, sortedResult.size()); i++) {
                String parentKey = sortedResult.get(i).getValue();
                int countInA = botSession.getTrieC().getCountForPrefix(parentKey);
                int countInB = botSession.getTrieD().getCountForPrefix(parentKey);
                log.info("foreach {} {} {}", parentKey, countInA, countInB);
                if (!(countInA == 0 && countInB == 0)) {
                    result
                            .append("`")
                            .append(parentKey)
                            .append("` : ")
                            .append(countInA - countInB)
                            .append("\n\n");
                }
            }

            if (setCombine.isEmpty()) {
                result.append("""
                        ```It seems you have reached the leaf node of the trie.
                        This trie does not store the token on the tail-end of the parsed keys.
                        For ex: the key "FOO:BAR:BAZ" will be stored as:
                        root
                          |___FOO
                               |___BAR```
                        """);
            }

            long endTime = System.currentTimeMillis();
            result.append("\n`query time: ").append(endTime - startTime).append(" ms`\n");

            setExitCode(0);
        } catch (Exception e) {
            log.info("Exception here: {}", e.getMessage());
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
