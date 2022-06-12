package org.example;

import java.util.HashMap;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.processing.Parser;
import org.trie.QTrie;

@Getter
@Builder
public class Channel {

    @Builder.Default
    static volatile HashMap<String, Channel> channels = new HashMap<>();

    @Builder.Default
    private volatile String dumpA = "../dump-A.rdb";

    @Builder.Default
    private volatile String dumpB = "../dump-B.rdb";

    @Builder.Default
    private volatile String keysA = "../keys-A.txt";

    @Builder.Default
    private volatile String keysB = "../keys-B.txt";

    @Setter
    @Builder.Default
    private volatile QTrie trieA = null;

    @Setter
    @Builder.Default
    private volatile QTrie trieB = null;

    @Builder.Default
    private volatile Parser parser = Parser.builder().build();

    public enum ParsingStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
    }

    public enum TrieStatus {
        NOT_CONSTRUCTED,
        CONSTRUCTING,
        CONSTRUCTED,
    }

    public enum InteractiveSessionStatus {
        //TODO: implement check so that command line interaction and interactive session cannot be used together
    }

    @Builder.Default
    public volatile ParsingStatus parsingStatus = ParsingStatus.NOT_STARTED;

    @Builder.Default
    public volatile TrieStatus trieStatus = TrieStatus.NOT_CONSTRUCTED;

    public static Channel getChannel(final String channelId) {
        if (!channels.containsKey(channelId)) {
            channels.put(channelId, Channel.builder().build());
        }
        return channels.get(channelId);
    }

    public static void removeChannel(final String channelId) {
        channels.remove(channelId);
    }
}
