package org.trie;

import static java.lang.Math.min;
import static org.example.Main.props;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class BotStringTokenizer {

    @Builder.Default
    private char[] tokens;

    @Builder.Default
    private int index = 0;

    @Builder.Default
    private int charArraySize = 0;

    @NonNull
    private final String path;

    @NonNull
    @Builder.Default
    private final int maxTrieDepth = Integer.parseInt(props.getProperty("MAX_TRIE_DEPTH_DEFAULT"));

    public BotStringTokenizer tokenize() {
        //        String[] tokensWithEmptyString = path.split(delimiter, -1);
        char[] charArrayTemp = path.toCharArray();
        charArraySize = min(charArrayTemp.length, (maxTrieDepth));
        tokens = new char[charArraySize];
        System.arraycopy(charArrayTemp, 0, tokens, 0, charArraySize);
        return this;
    }

    public char nextToken() {
        if (index >= charArraySize) {
            return '\0';
        }
        return tokens[index++];
    }

    public char startsWith() {
        if (charArraySize != 0) {
            return tokens[0];
        }
        return '\0';
    }

    public boolean hasMoreTokens() {
        return index < charArraySize;
    }
}
