package org.querying;

import static org.example.SlackHelper.trieA;
import static org.example.SlackHelper.trieB;

import java.util.List;
import java.util.Map;
import org.trie.QTrie;

public class nextKeyQuery extends Query {

    String key = "";
    int n = 1;
    StringBuilder result = new StringBuilder();

    public nextKeyQuery(String _key, int _n) {
        super(QueryType.TOP_K_CHILDREN);
        key = _key;
        n = _n;
    }

    public void execute() {
        System.out.println("executing nextKey query...!");
        try {
            for (QTrie trie : new QTrie[] { trieA, trieB }) {
                try {
                    List<Map.Entry<String, Integer>> query = trie.topNKeyWithPrefix(
                        key,
                        n
                    );
                    int found = query.size() - 2;
                    if (found < n) {
                        System.out.println(
                            "Found " +
                            found +
                            " prefixes only, less than the requested number of prefixes"
                        );
                    }
                    result
                        .append("Top ")
                        .append(found)
                        .append(" keys-prefixes with fixed prefix *")
                        .append(key)
                        .append("*: \n");
                    result
                        .append("Total keys with prefix *")
                        .append(key)
                        .append("*: ")
                        .append(query.get(0).getValue())
                        .append("\n");
                    result
                        .append("Top ")
                        .append(found)
                        .append(" key-prefixes with prefix: \"")
                        .append(key)
                        .append("\": \n");
                    for (int i = 2; i < query.size(); i++) {
                        result
                            .append(i - 1)
                            .append(". ")
                            .append(query.get(i).getKey())
                            .append(" : ")
                            .append(query.get(i).getValue())
                            .append(" keys.\n");
                    }
                    if (query.get(1).getValue() > found) {
                        result
                            .append("... and ")
                            .append(query.get(1).getValue() - found)
                            .append(" others...\n");
                    }
                } catch (Exception e) {
                    result
                        .append("No keys found for ")
                        .append(key)
                        .append(" in ")
                        .append(trie.getKeysFile())
                        .append("\n");
                }
                result.append("\n");
            }
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
