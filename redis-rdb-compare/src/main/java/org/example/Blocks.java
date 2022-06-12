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

public class Blocks {

    public static int INDEX = 0;

    private static String getIndex() {
        return String.valueOf(INDEX++);
    }

    public static LayoutBlock ButtonBlock(
        String buttonText,
        String buttonValue,
        String buttonStyle
    ) {
        return actions(actions ->
            actions.elements(
                asElements(
                    button(b ->
                        b
                            .text(plainText(buttonText))
                            .style(buttonStyle)
                            .value(buttonValue)
                            .actionId("buttonBlock-" + buttonValue + "-" + getIndex())
                    )
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
