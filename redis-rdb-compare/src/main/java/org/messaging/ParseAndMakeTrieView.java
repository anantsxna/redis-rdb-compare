package org.messaging;

import static org.messaging.Blocks.TextBlock;
import static org.messaging.Blocks.ThreeButtonBlock;
import static org.messaging.PostUpdateUtils.updateResponseSync;

import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.LayoutBlock;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import org.example.Channel;
import org.processing.Parser;
import org.trie.QTrie;

/**
 * A self updating class for dealing with a periodically changing response message.
 * Displays parsing and trie construction status in interactive mode.
 */
@Builder
public class ParseAndMakeTrieView {

    @NonNull
    private final String timestamp;

    @NonNull
    private final String channelId;

    @Builder.Default
    private final long startTime = System.currentTimeMillis();

    private Channel channel;

    @Builder.Default
    private long parseTime = 0;

    @Builder.Default
    private long makeTrieTime = 0;

    /**
     * Run the parsing and trie construction process.
     */
    public void run() {
        //execute parse, execute maketrie, periodically update the response
        System.out.println("Parsing and making trie for channel " + channelId);
        channel = Channel.getChannel(channelId);
        Parser parser = channel.getParser();
        parser.clear();
        parser.addToParser(channel.getDumpA(), channel.getKeysA());
        parser.addToParser(channel.getDumpB(), channel.getKeysB());
        channel.setParsingStatus(Channel.ParsingStatus.IN_PROGRESS);
        System.out.println("Parsing completed.");
        updateResponse();

        parser.parse();
        parseTime = System.currentTimeMillis() - startTime;
        channel.setParsingStatus(Channel.ParsingStatus.COMPLETED);
        System.out.println("Parsing completed in " + parseTime + " milliseconds");
        updateResponse();

        channel.setTrieA(QTrie.builder().keysFile(channel.getKeysA()).build());
        channel.setTrieB(QTrie.builder().keysFile(channel.getKeysB()).build());
        channel.trieStatus = Channel.TrieStatus.CONSTRUCTING;
        System.out.println("Trie construction started.");
        updateResponse();

        channel.getTrieA().takeInput();
        channel.getTrieB().takeInput();
        makeTrieTime = System.currentTimeMillis() - startTime - parseTime;
        channel.trieStatus = Channel.TrieStatus.CONSTRUCTED;
        System.out.println("Trie construction completed in " + makeTrieTime + " milliseconds");
        updateResponse();
    }

    /**
     * Calls the utility method to update response
     */
    public void updateResponse() {
        updateResponseSync(
            buildResponse(),
            channelId,
            "Response from parsing and making trie",
            timestamp
        );
    }

    /**
     * Constructs the response according to object state.
     * @return the response
     */
    private List<LayoutBlock> buildResponse() {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(DividerBlock.builder().build());
        if (channel.getParsingStatus().equals(Channel.ParsingStatus.NOT_STARTED)) {
            blocks.add(TextBlock("Parsing has not started yet."));
        } else if (channel.getParsingStatus().equals(Channel.ParsingStatus.IN_PROGRESS)) {
            blocks.add(TextBlock("Parsing..."));
        } else {
            blocks.add(TextBlock("Parsing completed."));
            blocks.add(TextBlock("Parsing time: " + parseTime + "ms"));
            if (channel.trieStatus.equals(Channel.TrieStatus.NOT_CONSTRUCTED)) {
                blocks.add(TextBlock("Tries have not begin construction yet..."));
            } else if (channel.trieStatus.equals(Channel.TrieStatus.CONSTRUCTING)) {
                blocks.add(TextBlock("Trie construction in progress..."));
            } else {
                blocks.add(DividerBlock.builder().build());
                blocks.add(TextBlock("Trie construction completed."));
                blocks.add(TextBlock("Trie construction time: " + makeTrieTime + "ms"));
                blocks.add(DividerBlock.builder().build());
                blocks.add(
                    ThreeButtonBlock(
                        "Get Count",
                        "queryAll-count",
                        "primary",
                        "Get Next",
                        "queryAll-next",
                        "primary",
                        "Reset",
                        "resetAll",
                        "danger",
                        "Any parsed data and tries will be deleted.",
                        "Reset",
                        "Cancel"
                    )
                );
                blocks.add(DividerBlock.builder().build());
            }
        }
        return blocks;
    }
}
