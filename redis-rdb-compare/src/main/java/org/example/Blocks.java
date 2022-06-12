package org.example;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.button;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.ConfirmationDialogObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElements;

public class Blocks {

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
                            .actionId("buttonBlock-" + buttonValue + "-" + randomAlphanumeric(10))
                    )
                )
            )
        );
    }

    public static LayoutBlock TwoButtonBlock(
        String buttonTextA,
        String buttonValueA,
        String buttonStyleA,
        String buttonTextB,
        String buttonValueB,
        String buttonStyleB
    ) {
        return actions(actions ->
            actions.elements(
                asElements(
                    button(b ->
                        b
                            .text(plainText(buttonTextA))
                            .style(buttonStyleA)
                            .value(buttonValueA)
                            .actionId("buttonBlock-" + buttonValueA + "-" + randomAlphanumeric(10))
                    ),
                    button(b ->
                        b
                            .text(plainText(buttonTextB))
                            .style(buttonStyleB)
                            .value(buttonValueB)
                            .actionId("buttonBlock-" + buttonValueB + "-" + randomAlphanumeric(10))
                    )
                )
            )
        );
    }

    public static LayoutBlock ThreeButtonBlock(
        String buttonTextA,
        String buttonValueA,
        String buttonStyleA,
        String buttonTextB,
        String buttonValueB,
        String buttonStyleB,
        String buttonTextC,
        String buttonValueC,
        String buttonStyleC
    ) {
        return actions(actions ->
            actions.elements(
                asElements(
                    button(b ->
                        b
                            .text(plainText(buttonTextA))
                            .style(buttonStyleA)
                            .value(buttonValueA)
                            .actionId("buttonBlock-" + buttonValueA + "-" + randomAlphanumeric(10))
                    ),
                    button(b ->
                        b
                            .text(plainText(buttonTextB))
                            .style(buttonStyleB)
                            .value(buttonValueB)
                            .actionId("buttonBlock-" + buttonValueB + "-" + randomAlphanumeric(10))
                    ),
                    button(b ->
                        b
                            .text(plainText(buttonTextC))
                            .style(buttonStyleC)
                            .value(buttonValueC)
                            .actionId("buttonBlock-" + buttonValueC + "-" + randomAlphanumeric(10))
                    )
                )
            )
        );
    }

    public static LayoutBlock ButtonWithConfirmBlock(
        String buttonText,
        String buttonValue,
        String buttonStyle,
        String warningText,
        String confirmText,
        String denyText
    ) {
        return actions(actions ->
            actions.elements(
                asElements(
                    button(b ->
                        b
                            .text(plainText(buttonText))
                            .style(buttonStyle)
                            .value(buttonValue)
                            .actionId("buttonBlock-" + buttonValue + "-" + randomAlphanumeric(10))
                            .confirm(
                                ConfirmationDialogObject
                                    .builder()
                                    .title(PlainTextObject.builder().text("Are you sure?").build())
                                    .text(PlainTextObject.builder().text(warningText).build())
                                    .confirm(PlainTextObject.builder().text(confirmText).build())
                                    .deny(PlainTextObject.builder().text(denyText).build())
                                    .style("primary")
                                    .build()
                            )
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
