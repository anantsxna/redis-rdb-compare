package org.querying;

import static org.example.BotSession.getBotSession;
import static org.example.Main.props;
import static org.messaging.Blocks.*;

import com.slack.api.model.block.LayoutBlock;
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
            if (key.equals("!root")) {
                key = "";
            }

            text.append(">In session ").append(getRequestId());
            if (key.equals("")) {
                text.append("\n\nFor both the tries");
            } else {
                text.append("\n\nFor query: *").append(key).append("*");
            }
            text
                .append(", with change: ")
                .append(
                    botSession.getTrieB().getCountForPrefix(key) -
                    botSession.getTrieA().getCountForPrefix(key)
                )
                .append("\n\n");
            result.add(TextBlock(text.toString()));
            text.setLength(0);

            long startTime = System.currentTimeMillis();

            assert botSession != null;

            Set<String> setA = botSession.getTrieA().getChildren(key);
            Set<String> setB = botSession.getTrieB().getChildren(key);

            Set<String> setCombine = new HashSet<>();
            setCombine.addAll(setA);
            setCombine.addAll(setB);

            List<Map.Entry<Integer, String>> sortedResult = new ArrayList<>();
            for (String parentKey : setCombine) {
                int countInA = botSession.getTrieA().getCountForPrefix(parentKey);
                int countInB = botSession.getTrieB().getCountForPrefix(parentKey);
                sortedResult.add(new AbstractMap.SimpleEntry<>((countInB - countInA), parentKey));
            }

            Collections.sort(sortedResult, Comparator.comparing(p -> -Math.abs(p.getKey())));

            for (int i = 0; i < Math.min(head, sortedResult.size()); i++) {
                String parentKey = sortedResult.get(i).getValue();
                int countInA = botSession.getTrieA().getCountForPrefix(parentKey);
                int countInB = botSession.getTrieB().getCountForPrefix(parentKey);
                log.info("foreach {} {} {}", parentKey, countInA, countInB);
                if (!(countInA == 0 && countInB == 0)) {
                    text
                        .append("`")
                        .append(parentKey)
                        .append("` : ")
                        .append(countInB - countInA)
                        .append("\n\n");
                    result.add(
                        ButtonWithTextBlock(
                            text.toString(),
                            "Search",
                            "query-search-getcount-response-" +
                            getRequestId() +
                            "%" +
                            parentKey +
                            "%" +
                            head,
                            "primary"
                        )
                    );
                    text.setLength(0);
                }
            }

            if (setCombine.isEmpty()) {
                text.append(
                    """
                                ```It seems you have reached the leaf node of the trie.
                                This trie does not store the chars more than the max trie depth (default 100) of the parsed keys.
                                For ex: the key "FOOBAR" and depth 4 will be stored as:
                                root
                                  |___F
                                      |___O
                                          |___O
                                              |___B```
                                """
                );
                result.add(TextBlock(text.toString()));
                text.setLength(0);
            }

            long endTime = System.currentTimeMillis();
            text.append("\n`query time: ").append(endTime - startTime).append(" ms`\n");
            result.add(TextBlock(text.toString()));
            text.setLength(0);

            //            log.info("key {} smallkey {}", key, key.substring(0, key.length() - 1));
            String backKey;

            if (key.length() > 1 && !key.equals("!root")) backKey =
                key.substring(0, key.length() - 1); else backKey = "!root";
            if (!key.equals("") && !key.equals("!root")) {
                result.add(
                    ButtonBlock(
                        "Go Back",
                        "query-search-getcount-response-" +
                        getRequestId() +
                        "%" +
                        backKey +
                        "%" +
                        head,
                        "danger"
                    )
                );
            }
            setExitCode(0);
        } catch (Exception e) {
            log.info("Exception here: {}", e.getMessage());
            text.append(INVALID_REQUEST_ID);
            result.add(TextBlock(text.toString()));
            text.setLength(0);
            setExitCode(0);
        }
    }

    @Override
    public List<LayoutBlock> result() {
        if (getExitCode() == 0) {
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
