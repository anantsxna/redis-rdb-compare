package org.views;

import static com.slack.api.model.block.element.BlockElements.asElements;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.messaging.Blocks.ButtonBlock;
import static org.messaging.Blocks.TwoButtonBlock;
import static org.messaging.PostUpdateUtils.postResponseAsync;
import static org.messaging.PostUpdateUtils.updateResponseAsync;

import com.slack.api.model.block.*;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.PlainTextInputElement;
import com.slack.api.model.block.element.StaticSelectElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.BotSession;

/**
 * MENU INTERACTIVE VIEW
 * - home page
 * - select session
 * - create new session
 */
@Slf4j
@Builder
public class MenuView {

    @NonNull
    private final String channelId;

    private final String messageTs;

    /**
     * posts interactive message if called from slash command or @mention
     * updates interactive message, otherwise
     */
    public void start() {
        log.info("MenuView for channel " + channelId);
        if (messageTs == null) postResponseAsync(getMenuResponse(), channelId, "OK 200"); else {
            updateResponseAsync(getMenuResponse(), channelId, "OK 200", messageTs);
        }
    }

    /**
     * @return the response in the form of the List of LayoutBlocks
     */
    public List<LayoutBlock> getMenuResponse() {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(
            HeaderBlock
                .builder()
                .text(PlainTextObject.builder().text(":wave: Redis Database Bot").build())
                .build()
        );

        blocks.add(DividerBlock.builder().build());

        String placeholder = "Select a Request Id";
        if (BotSession.getAllBotSessions().isEmpty()) placeholder = "No active requests";

        blocks.add(
            ActionsBlock
                .builder()
                .elements(
                    asElements(
                        StaticSelectElement
                            .builder()
                            .placeholder(PlainTextObject.builder().text(placeholder).build())
                            .actionId("buttonBlock-query-view-select-" + randomAlphanumeric(10))
                            .options(allBotSessionsBlock())
                            .build()
                    )
                )
                .build()
        );

        blocks.add(DividerBlock.builder().build());

        if (messageTs != null) {
            blocks.add(
                InputBlock
                    .builder()
                    .label(PlainTextObject.builder().text("Creating New Session").build())
                    .optional(false)
                    .dispatchAction(true)
                    .element(
                        PlainTextInputElement
                            .builder()
                            .actionId("inputBlock-process-" + randomAlphanumeric(10))
                            .placeholder(
                                PlainTextObject
                                    .builder()
                                    .text(
                                        "Enter the links for databases and max trie depth, space-separated:\n\n[S3Link_1] [S3Link_2 (optional)] [maxTrieDepth (optional, default: 100)]"
                                    )
                                    .build()
                            )
                            .multiline(true)
                            .build()
                    )
                    .build()
            );

            blocks.add(ButtonBlock("Close", "delete-message", "danger"));
        } else {
            blocks.add(
                TwoButtonBlock(
                    "New Session",
                    "create-new-session",
                    "primary",
                    "Close",
                    "delete-message",
                    "danger"
                )
            );
        }
        return blocks;
    }

    /**
     * @return the list of options for the select element
     */
    public List<OptionObject> allBotSessionsBlock() {
        List<OptionObject> options = new ArrayList<>();
        int index = 1;

        for (Map.Entry<String, BotSession> entry : BotSession.getAllBotSessions().entrySet()) {
            String responseId = entry.getKey();
            BotSession botSession = entry.getValue();
            if (botSession.getTrieMakingStatus() != BotSession.TrieMakingStatus.CONSTRUCTED) {
                continue;
            }
            options.add(
                OptionObject
                    .builder()
                    .text(PlainTextObject.builder().text(index + ". " + responseId).build())
                    .value(responseId)
                    .build()
            );
            index++;
        }

        if (index == 1) {
            options.add(
                OptionObject
                    .builder()
                    .text(PlainTextObject.builder().text("No active requests").build())
                    .build()
            );
        }
        return options;
    }
}
