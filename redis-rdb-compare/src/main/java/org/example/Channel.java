package org.example;

import java.util.HashMap;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.processing.Parser;
import org.trie.QTrie;

/**
 * Channel Class
 * Maps a channel id to dumps files, keys files, a parser and tries and maintains their status.
 */
@Getter
@Builder
public class Channel {

    @Builder.Default
    static volatile HashMap<String, Channel> channels = new HashMap<>(); //static map of channel ids to channels

    @Builder.Default
    private final String dumpA = "../dump-A.rdb";

    @Builder.Default
    private final String dumpB = "../dump-B.rdb";

    @Builder.Default
    private final String keysA = "../keys-A.txt";

    @Builder.Default
    private final String keysB = "../keys-B.txt";

    @Setter
    @Builder.Default
    private QTrie trieA = null;

    @Setter
    @Builder.Default
    private QTrie trieB = null;

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

    @Builder.Default
    @Setter
    private volatile ParsingStatus parsingStatus = ParsingStatus.NOT_STARTED;

    @Builder.Default
    @Setter
    public volatile TrieStatus trieStatus = TrieStatus.NOT_CONSTRUCTED;

    /**
     * Getter for the channel.
     * If the channel id is not set, a new channel is created.
     * @param channelId: the id for the required channel
     * @return Channel object
     */
    public static Channel getChannel(final String channelId) {
        if (!channels.containsKey(channelId)) {
            channels.put(channelId, Channel.builder().build());
        }
        return channels.get(channelId);
    }

    /**
     * Remove a channel from the channels map.
     * @param channelId: the id for the channel to be removed
     */
    public static void removeChannel(final String channelId) {
        channels.remove(channelId);
    }
}
