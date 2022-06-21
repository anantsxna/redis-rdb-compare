package org.views;

import static org.example.BotSession.createBotSession;
import static org.example.SlackUtils.*;
import static org.messaging.Blocks.TextImageBlock;
import static org.messaging.Blocks.TwoButtonBlock;
import static org.messaging.PostUpdateUtils.updateResponseSync;

import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.PlainTextObject;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * A self updating class for dealing with a periodically changing response message.
 * Displays parsing and trie construction status in interactive mode.
 */
@Slf4j
@Builder
public class ProcessView {

    @NonNull
    private final String channelId;

    private String requestId;

    @NonNull
    private final String messageTs;

    @NonNull
    private final String userInput;

    @NonNull
    private final List<LayoutBlock> blocks = new ArrayList<>();

    private static final String REDIS_LOGO_URL =
        "https://avatars.githubusercontent.com/u/1529926?s=200&v=4";
    private static final String SESSION_IN_PROGRESS = "A session is already open.\n";
    private static final String DOWNLOADING_NOT_COMPLETED = "Downloading not completed.";
    private static final String PARSING_NOT_COMPLETED =
        "Parsing not done. Please wait for parsing to finish or use \"/parse\" command to start parsing.";
    private static final String TRIE_CONSTRUCTION_NOT_COMPLETED = "Trie construction completed";
    private static final String ALL_PROCESSING_DONE =
        "Processing done. Files Downloaded, Parsed and Made into Tries.\nReady to answer queries.\n";
    private static final String SESSION_CREATED =
        "A session has been created. Ready to parse and make tries.\n";

    /**
     * Run the downloading, parsing and trie construction process.
     */
    public void start() {
        log.info("ProcessView for channel : {}", channelId);
        requestId = createBotSession();
        if (requestId == null) {
            updateWithAddedBlock(TextImageBlock(SESSION_IN_PROGRESS, REDIS_LOGO_URL));
            return;
        }

        updateWithAddedBlock(
            HeaderBlock
                .builder()
                .text(PlainTextObject.builder().text("Request Id : " + requestId).build())
                .build()
        );

        updateWithAddedBlock(DividerBlock.builder().build());

        updateWithAddedBlock(
            TextImageBlock(
                SESSION_CREATED + "\n\n\n>Generated Request Id: " + requestId,
                REDIS_LOGO_URL
            )
        );

        final String downloadComplete = downloadUtils(requestId + " " + userInput, channelId, true);
        if (!downloadComplete.contains("Downloading completed")) {
            updateWithAddedBlock(
                TextImageBlock(
                    downloadComplete + "\n\n\n" + DOWNLOADING_NOT_COMPLETED,
                    REDIS_LOGO_URL
                )
            );
            deleteSessionUtils(requestId);
            return;
        }

        updateWithAddedBlock(TextImageBlock(downloadComplete, REDIS_LOGO_URL));

        final String parsingComplete = parseUtils(requestId, channelId, true);
        if (!parsingComplete.contains("Parsing completed")) {
            updateWithAddedBlock(
                TextImageBlock(parsingComplete + "\n\n\n" + PARSING_NOT_COMPLETED, REDIS_LOGO_URL)
            );
            deleteSessionUtils(requestId);
            return;
        }

        updateWithAddedBlock(TextImageBlock(parsingComplete, REDIS_LOGO_URL));

        final String trieComplete = makeTrieUtils(requestId, channelId, true);
        if (!trieComplete.contains("Trie construction completed")) {
            updateWithAddedBlock(
                TextImageBlock(
                    trieComplete + "\n\n\n" + TRIE_CONSTRUCTION_NOT_COMPLETED,
                    REDIS_LOGO_URL
                )
            );
            deleteSessionUtils(requestId);
            return;
        }

        updateWithAddedBlock(TextImageBlock(trieComplete, REDIS_LOGO_URL));

        updateWithAddedBlock(
            TextImageBlock(
                ALL_PROCESSING_DONE + "\n\n\n>Your Request Id: " + requestId,
                REDIS_LOGO_URL
            )
        );
        updateWithAddedBlock(DividerBlock.builder().build());
        updateWithAddedBlock(
            TwoButtonBlock(
                "Query",
                "query-view-click",
                "primary",
                "Close",
                "delete-message",
                "danger"
            )
        );
    }

    /**
     * Calls the utility method to update response
     */
    public void updateWithAddedBlock(LayoutBlock block) {
        blocks.add(block);
        updateResponseSync(blocks, channelId, requestId, messageTs);
        //        blocks.remove(blocks.size() - 1);
    }
}
