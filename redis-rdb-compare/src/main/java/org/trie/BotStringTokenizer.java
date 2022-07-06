package org.trie;

import static org.example.Main.props;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class BotStringTokenizer {

    @NonNull
    @Builder.Default
    private final String delimiter = props.getProperty("DELIMITER");

    @NonNull
    @Builder.Default
    private final List<String> tokens = new ArrayList<>();

    @Builder.Default
    private int index = 0;

    @NonNull
    private final String path;

    public BotStringTokenizer tokenize() {
        String[] tokensWithEmptyString = path.split(delimiter, -1);
        boolean previousTokenEmpty = false;
        for (String token : tokensWithEmptyString) {
            if (token.isEmpty()) {
                previousTokenEmpty = true;
                continue;
            }
            tokens.add((previousTokenEmpty ? delimiter : "") + token);
            previousTokenEmpty = false;
        }
        return this;
    }

    public String nextToken() {
        if (index >= tokens.size()) {
            return null;
        }
        return tokens.get(index++);
    }

    public String startsWith() {
        if (!tokens.isEmpty()) {
            return tokens.get(0);
        }
        return null;
    }

    public boolean hasMoreTokens() {
        return index < tokens.size();
    }
}
