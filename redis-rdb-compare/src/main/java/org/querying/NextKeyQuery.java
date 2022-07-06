package org.querying;

import static org.example.BotSession.getBotSession;

import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.example.BotSession;
import org.trie.QTrie;

/**
 * "/getnext [prefixKey] [n]" query.
 */
@Slf4j
@SuperBuilder
public class NextKeyQuery extends Query {

    @NonNull
    String key;

    int n;

    @Override
    public void execute() {
        try {
            BotSession botSession = getBotSession(getRequestId());
            try {
                result.append(">In session ").append(getRequestId()).append("\n\n");
                for (QTrie trie : new QTrie[] { botSession.getTrieC() }) {
                    try {
                        long startTime = System.currentTimeMillis();
                        List<Map.Entry<String, Integer>> query = trie.topNKeyWithPrefix(key, n);
                        long endTime = System.currentTimeMillis();
                        int found = query.size() - 2;

                        result.append("Total keys with prefix *").append(key).append("*: ");
                        if (trie == botSession.getTrieA()) {
                            result.append("in first database: *");
                        } else if (trie == botSession.getTrieB()) {
                            result.append("in second database: *");
                        } else {
                            result.append("in diff file: *");
                        }
                        result.append(query.get(0).getValue());
                        result.append("*\n");
                        if (query.get(0).getValue() > 0 && found == 0) {
                            result.append(
                                """
                                            ```It seems you have reached the leaf node of the trie.
                                            This trie does not store the token on the tail-end of the parsed keys.
                                            For ex: the key "FOO:BAR:BAZ" will be stored as:
                                            root
                                              |___FOO
                                                   |___BAR```
                                            """
                            );
                            continue;
                        }
                        if (found < n) {
                            result
                                .append("`WARN: Found ")
                                .append(found)
                                .append(" prefixes only, less than the requested number (")
                                .append(n)
                                .append(")`\n");
                        }
                        result
                            .append(">Top ")
                            .append(found)
                            .append(" key-prefixes that start with: \"")
                            .append(key)
                            .append("\": \n");
                        for (int i = 2; i < query.size(); i++) {
                            result
                                .append(">")
                                .append(i - 1)
                                .append(". ")
                                .append(query.get(i).getKey())
                                .append(" : ")
                                .append(query.get(i).getValue())
                                .append(" key(s).\n");
                        }
                        if (query.get(1).getValue() > found) {
                            result
                                .append(">... and ")
                                .append(query.get(1).getValue() - found)
                                .append(" others...\n");
                        }
                        result
                            .append("`query time: ")
                            .append(endTime - startTime)
                            .append(" ms`\n\n\n");
                    } catch (Exception e) {
                        result
                            .append(">No keys found for ")
                            .append(key)
                            .append(" in database.")
                            .append("\n");
                    }
                    log.info("Next query for key: {} in botSession: {}", key, getRequestId());
                }
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
            log.info("result here: \n {}", result.toString());
            return result.toString();
        } else if (getExitCode() == -1) {
            return "Error: Query could not execute";
        } else {
            return "Error: Query failed";
        }
    }
}
