package org.trie;

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
    private final String delimiter = ":";

    @NonNull
    @Builder.Default
    private final List<String> tokens = new ArrayList<>();

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

    public boolean hasMoreTokens() {
        return index < tokens.size();
    }
}
