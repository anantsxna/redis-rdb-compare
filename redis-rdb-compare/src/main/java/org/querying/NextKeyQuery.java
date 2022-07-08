package org.querying;

import static org.example.BotSession.getBotSession;
import static org.messaging.Blocks.TextBlock;

import com.slack.api.model.block.LayoutBlock;
import java.util.ArrayList;
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
                text.append(">In session ").append(getRequestId()).append("\n\n");
                for (QTrie trie : new QTrie[] { botSession.getTrieA() }) {
                    try {
                        long startTime = System.currentTimeMillis();
                        List<Map.Entry<String, Integer>> query = trie.topNKeyWithPrefix(key, n);
                        long endTime = System.currentTimeMillis();
                        int found = query.size() - 2;

                        text.append("Total keys with prefix *").append(key).append("*: ");
                        if (trie == botSession.getTrieA()) {
                            text.append("in first database: *");
                        } else if (trie == botSession.getTrieB()) {
                            text.append("in second database: *");
                        } else {
                            text.append("in diff file: *");
                        }
                        text.append(query.get(0).getValue());
                        text.append("*\n");
                        if (query.get(0).getValue() > 0 && found == 0) {
                            text.append(
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
                            text
                                .append("`WARN: Found ")
                                .append(found)
                                .append(" prefixes only, less than the requested number (")
                                .append(n)
                                .append(")`\n");
                        }
                        text
                            .append(">Top ")
                            .append(found)
                            .append(" key-prefixes that start with: \"")
                            .append(key)
                            .append("\": \n");
                        for (int i = 2; i < query.size(); i++) {
                            text
                                .append(">")
                                .append(i - 1)
                                .append(". ")
                                .append(query.get(i).getKey())
                                .append(" : ")
                                .append(query.get(i).getValue())
                                .append(" key(s).\n");
                        }
                        if (query.get(1).getValue() > found) {
                            text
                                .append(">... and ")
                                .append(query.get(1).getValue() - found)
                                .append(" others...\n");
                        }
                        text
                            .append("`query time: ")
                            .append(endTime - startTime)
                            .append(" ms`\n\n\n");
                    } catch (Exception e) {
                        text
                            .append(">No keys found for ")
                            .append(key)
                            .append(" in database.")
                            .append("\n");
                    }
                    log.info("Next query for key: {} in botSession: {}", key, getRequestId());
                }
            } catch (Exception e) {
                text.append(">The key does not exist in the database.");
            }
            setExitCode(0);
        } catch (Exception e) {
            text.append(INVALID_REQUEST_ID);
            setExitCode(0);
        }
    }

    @Override
    public List<LayoutBlock> result() {
        if (getExitCode() == 0) {
            result.add(TextBlock(text.toString()));
            text.setLength(0);
            return result;
        } else if (getExitCode() == -1) {
            text.append("Error: Query could not execute");
            result.add(TextBlock(text.toString()));
            text.setLength(0);
            return result;
        } else {
            text.append("Error: Query failed");
            result.add(TextBlock(text.toString()));
            text.setLength(0);
            return result;
        }
    }
}
