package org.messaging;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.messaging.Blocks.*;
import static org.messaging.PostUpdateUtils.*;

import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.InputBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.PlainTextInputElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains methods for posting, updating and deleting messages asynchronously and synchronously.
 * Methods invoke utility methods from PostUpdateUtils
 * after constructing the message with methods from Blocks.
 */
public class PostUpdate {

    private static final String REDIS_LOGO_URL =
        "https://avatars.githubusercontent.com/u/1529926?s=200&v=4";

    /**
     * Post a text message to the channel.
     *
     * @param responseMessage: the message to post
     * @param channelId:       the botSession to post to
     */
    public static void postTextResponseAsync(String responseMessage, final String channelId) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(TextImageBlock(responseMessage, REDIS_LOGO_URL));
        postResponseAsync(blocks, channelId, responseMessage);
    }

    /**
     * Post a text response to the channel
     *
     * @param responseMessage: the message to post
     * @param channelId:       the botSession to post to
     */
    public static void postTextResponseSync(String responseMessage, final String channelId) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(TextImageBlock(responseMessage, REDIS_LOGO_URL));
        postResponseSync(blocks, channelId, responseMessage);
    }

    /**
     * Update a text message in a botSession.
     *
     * @param responseMessage: the new updated text message
     * @param channelId:       the botSession to update
     * @param timestamp:       the timestamp of the message to update
     */
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

    /**
     * Post a text message and a button to reset the botSession
     *
     * @param responseMessage: the message to post
     * @param channelId:       the botSession to post to
     */
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

    /**
     * Update a message with text adn a button to reset the botSession
     *
     * @param responseMessage: the message to update
     * @param channelId:       the botSession to update
     * @param timestamp:       the timestamp of the message to update
     */
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

    /**
     * Post the message at the start of an interactive session
     *
     * @param responseMessage: the welcome message
     * @param channelId:       the botSession to post to
     */
    public static void postStartButtonResponse(String responseMessage, final String channelId) {
        postTwoButtonsWithHeaderResponseAsync(
            responseMessage,
            "Parse and Make Tries",
            "parseAndMakeTrieAll",
            "primary",
            "Close",
            "exitAll",
            "danger",
            channelId
        );
    }

    /**
     * Update a given message to the start of an interactive session after deleting previous session
     *
     * @param information:     the session has been deleted message
     * @param responseMessage: the welcome again message
     * @param channelId:       the botSession to update
     * @param timestamp:       the timestamp of the message to update
     */
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
            "parseAndMakeTrieAll",
            "primary",
            "Close",
            "exitAll",
            "danger",
            channelId,
            timestamp
        );
    }

    /**
     * Close the interactive session and delete the message
     */
    public static void deleteStartButtonResponse(final String channelId, String timestamp) {
        deleteResponseAsync(channelId, timestamp);
    }

    /**
     * Post a text message and a button
     *
     * @param responseMessage: the message to post
     * @param buttonText:      the text of the button
     * @param buttonValue:     a keyword to name the actionId (to catch blockActions payload later)
     * @param buttonStyle:     default/primary/danger
     * @param channelId:       the botSession to post to
     */
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

    /**
     * Update a message with a text message and a button
     *
     * @param responseMessage: the new updated text message
     * @param buttonTextA      : the text of the first button
     * @param buttonValueA:    a keyword to name the actionId (to catch blockActions payload later)
     * @param buttonStyleA:    default/primary/danger
     * @param channelId:       the botSession to update
     * @param timestamp:       the timestamp of the message to update
     */
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

    /**
     * Post a text header and two buttons
     *
     * @param responseMessage: the message to post
     * @param buttonTextA      : the text of the first button
     * @param buttonValueA:    a keyword to name the actionId (to catch blockActions payload later)
     * @param buttonStyleA:    default/primary/danger
     * @param channelId:       the botSession to update
     */
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

    /**
     * Update a message with the "/getcount" query's interactive response
     * Also displays the result of the previous query
     *
     * @param responseMessage: the message to update
     * @param channelId:       the botSession to update
     * @param timestamp:       the timestamp of the message to update
     */
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
                "Reset.",
                "resetAll",
                "danger",
                "Any parsed data and tries will be deleted.",
                "Reset",
                "Cancel"
            )
        );
        updateResponseAsync(blocks, channelId, responseMessage, timestamp);
    }

    /**
     * Update a message with the "/getnext" query's interactive response
     * Also displays the result of the previous query
     *
     * @param responseMessage: the message to update
     * @param channelId:       the botSession to update
     * @param timestamp:       the timestamp of the message to update
     */
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
                "Reset",
                "resetAll",
                "danger",
                "Any parsed data and tries will be deleted.",
                "Reset",
                "Cancel"
            )
        );
        updateResponseAsync(blocks, channelId, responseMessage, timestamp);
    }
}
