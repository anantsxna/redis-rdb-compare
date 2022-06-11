package org.example;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.button;

import com.slack.api.Slack;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.element.BlockElements;
import java.util.List;

public class SlackPostUtils {

    public static void postResponseAsync(
        List<LayoutBlock> layoutBlocks,
        String channelId,
        String responseMessage
    ) {
        Slack slack = Slack.getInstance();
        String token = System.getenv("SLACK_BOT_TOKEN");
        slack
            .methodsAsync(token)
            .chatPostMessage(req ->
                req.channel(channelId).text(responseMessage).blocks(layoutBlocks)
            )
            .thenAcceptAsync(res -> {
                if (res.isOk()) {
                    System.out.println("Message posted successfully.");
                } else {
                    String errorCode = res.getError(); // e.g., "invalid_auth", "channel_not_found"
                    System.out.println("Error posting message: " + errorCode);
                    throw new RuntimeException("Error posting message: " + errorCode);
                }
            });
    }

    public static LayoutBlock ButtonBlock(
        String buttonText,
        String buttonValue,
        String buttonStyle
    ) {
        return actions(actions ->
            actions.elements(
                asElements(
                    button(b -> b.text(plainText(buttonText)).style(buttonStyle).value(buttonValue))
                )
            )
        );
    }

    public static LayoutBlock ImageBlock(String imageUrl, String altText) {
        return section(section ->
            section
                .text(markdownText(altText))
                .accessory(BlockElements.image(image -> image.imageUrl(imageUrl).altText(altText)))
        );
    }

    public static LayoutBlock TextBlock(String text) {
        return section(section -> section.text(markdownText(text)));
    }

    public static LayoutBlock TextImageBlock(String text, String imageUrl) {
        return section(section ->
            section
                .text(markdownText(text))
                .accessory(BlockElements.image(image -> image.imageUrl(imageUrl).altText(text)))
        );
    }
}
