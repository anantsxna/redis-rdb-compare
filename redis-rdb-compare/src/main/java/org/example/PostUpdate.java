package org.example;

import static com.slack.api.model.block.Blocks.*;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.example.Blocks.*;
import static org.example.PostUpdateUtils.*;

import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.InputBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.PlainTextInputElement;
import java.util.ArrayList;
import java.util.List;

public class PostUpdate {

    private static final String REDIS_LOGO_URL =
        "https://avatars.githubusercontent.com/u/1529926?s=200&v=4";

    public static void postTextResponseAsync(String responseMessage, final String channelId) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(TextImageBlock(responseMessage, REDIS_LOGO_URL));
        postResponseAsync(blocks, channelId, responseMessage);
    }

    public static void updateTextResponseAsync(
        String responseMessage,
        final String channelId,
        String timestamp
    ) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(TextBlock(responseMessage));
        blocks.add(TextImageBlock(responseMessage, REDIS_LOGO_URL));
        updateResponseAsync(blocks, channelId, responseMessage, timestamp);
    }

    public static void postResetButtonResponseAsync(
        String responseMessage,
        final String channelId
    ) {
        postButtonWithTextResponseAsync(
            responseMessage,
            "Reset Bot and Start Fresh?",
            "resetAll",
            "danger",
            channelId
        );
    }

    public static void updateResetButtonResponseAsync(
        String responseMessage,
        final String channelId,
        String timestamp
    ) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(TextBlock(responseMessage));
        blocks.add(
            ButtonWithConfirmBlock(
                "Reset Bot and Start Fresh?",
                "resetAll",
                "danger",
                "The parsed data and tries will be deleted.",
                "Reset",
                "Cancel"
            )
        );
        updateResponseAsync(blocks, channelId, responseMessage, timestamp);
    }

    public static void postStartButtonResponse(String responseMessage, final String channelId) {
        postTwoButtonsWithHeaderResponseAsync(
            responseMessage,
            "Parse and Make Tries",
            "startAll",
            "primary",
            "Close",
            "exitAll",
            "danger",
            channelId
        );
    }

    public static void updateStartButtonResponse(
        String information,
        String responseMessage,
        final String channelId,
        String timestamp
    ) {
        updateTwoButtonsWithTextAndHeaderResponseAsync(
            information,
            responseMessage,
            "Parse and Make Tries",
            "startAll",
            "primary",
            "Close",
            "exitAll",
            "danger",
            channelId,
            timestamp
        );
    }

    public static void deleteStartButtonResponse(final String channelId, String timestamp) {
        deleteResponseAsync(channelId, timestamp);
    }

    public static void postButtonWithTextResponseAsync(
        String responseMessage,
        String buttonText,
        String buttonValue,
        String buttonStyle,
        final String channelId
    ) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(TextBlock(responseMessage));
        blocks.add(Blocks.ButtonBlock(buttonText, buttonValue, buttonStyle));
        postResponseAsync(blocks, channelId, responseMessage);
    }

    public static void updateTwoButtonsWithTextAndHeaderResponseAsync(
        String responseMessage,
        String headerText,
        String buttonTextA,
        String buttonValueA,
        String buttonStyleA,
        String buttonTextB,
        String buttonValueB,
        String buttonStyleB,
        final String channelId,
        String timestamp
    ) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(TextBlock(responseMessage));
        blocks.add(
            HeaderBlock.builder().text(PlainTextObject.builder().text(headerText).build()).build()
        );
        blocks.add(
            TwoButtonBlock(
                buttonTextA,
                buttonValueA,
                buttonStyleA,
                buttonTextB,
                buttonValueB,
                buttonStyleB
            )
        );
        updateResponseAsync(blocks, channelId, responseMessage, timestamp);
    }

    public static void postTwoButtonsWithHeaderResponseAsync(
        String responseMessage,
        String buttonTextA,
        String buttonValueA,
        String buttonStyleA,
        String buttonTextB,
        String buttonValueB,
        String buttonStyleB,
        final String channelId
    ) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(
            HeaderBlock
                .builder()
                .text(PlainTextObject.builder().text(responseMessage).build())
                .build()
        );
        blocks.add(
            TwoButtonBlock(
                buttonTextA,
                buttonValueA,
                buttonStyleA,
                buttonTextB,
                buttonValueB,
                buttonStyleB
            )
        );
        postResponseAsync(blocks, channelId, responseMessage);
    }

    public static void updateQueryCountResponseAsync(
        String responseMessage,
        final String channelId,
        String timestamp
    ) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(
            HeaderBlock
                .builder()
                .text(PlainTextObject.builder().text("Count of a Key").build())
                .build()
        );
        blocks.add(DividerBlock.builder().build());
        //TODO: change this condition
        if (!responseMessage.isEmpty()) {
            blocks.add(TextBlock(responseMessage));
            blocks.add(DividerBlock.builder().build());
        }
        blocks.add(
            InputBlock
                .builder()
                .label(PlainTextObject.builder().text("Search:").build())
                .element(
                    PlainTextInputElement
                        .builder()
                        .placeholder(PlainTextObject.builder().text("Prefix Key").build())
                        .actionId("inputBlock-countQuery-" + randomAlphanumeric(10))
                        .focusOnLoad(true)
                        .build()
                )
                .optional(false)
                .dispatchAction(true)
                .build()
        );
        blocks.add(
            ThreeButtonBlock(
                "Get Count",
                "queryAll-count",
                "primary",
                "Get Next",
                "queryAll-next",
                "primary",
                "Exit.",
                "resetAll",
                "danger"
            )
        );
        updateResponseAsync(blocks, channelId, responseMessage, timestamp);
    }

    public static void updateQueryNextResponseAsync(
        String responseMessage,
        final String channelId,
        String timestamp
    ) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(
            HeaderBlock
                .builder()
                .text(PlainTextObject.builder().text("Next Most Likely Keys").build())
                .build()
        );
        blocks.add(DividerBlock.builder().build());
        //TODO: change this condition
        if (!responseMessage.isEmpty()) {
            blocks.add(TextBlock(responseMessage));
            blocks.add(DividerBlock.builder().build());
        }
        blocks.add(
            InputBlock
                .builder()
                .label(PlainTextObject.builder().text("Search:").build())
                .element(
                    PlainTextInputElement
                        .builder()
                        .placeholder(PlainTextObject.builder().text("[Prefix Key] [count]").build())
                        .actionId("inputBlock-nextQuery-" + randomAlphanumeric(10))
                        .focusOnLoad(true)
                        .build()
                )
                .optional(false)
                .dispatchAction(true)
                .build()
        );
        blocks.add(
            ThreeButtonBlock(
                "Get Count",
                "queryAll-count",
                "primary",
                "Get Next",
                "queryAll-next",
                "primary",
                "Exit.",
                "resetAll",
                "danger"
            )
        );
        updateResponseAsync(blocks, channelId, responseMessage, timestamp);
    }
}
