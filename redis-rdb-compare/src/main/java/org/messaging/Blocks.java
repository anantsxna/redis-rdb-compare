package org.messaging;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.button;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.ConfirmationDialogObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElements;

/**
 * Methods for specific blocks used in responses.
 */
public class Blocks {

    /**
     * @param buttonText: the text of the button
     * @param buttonValue: the value used to make the actionId of the payload the button will send back
     * @param buttonStyle: default/primary/danger
     * @return button element
     */
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

    /**
     * @param buttonTextA: the text of the first button
     * @param buttonValueA: the value used to make the actionId of the payload the first button will send back
     * @param buttonStyleA: default/primary/danger
     * @return an action block with 2 button
     */
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

    /**
     * @param buttonTextA: the text of the first button
     * @param buttonValueA: the value used to make the actionId of the payload the first button will send back
     * @param buttonStyleA: default/primary/danger
     * @param warningText: the warning in the confirm dialog
     * @param confirmText: the text of the confirm button
     * @param denyText: the text of the deny button
     * @return an action block with 3 buttons, the third button will open a confirm dialog
     */
    public static LayoutBlock ThreeButtonBlock(
        String buttonTextA,
        String buttonValueA,
        String buttonStyleA,
        String buttonTextB,
        String buttonValueB,
        String buttonStyleB,
        String buttonTextC,
        String buttonValueC,
        String buttonStyleC,
        String warningText,
        String confirmText,
        String denyText
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

    /**
     * @param buttonText: the text of the button
     * @param buttonValue: the value used to make the actionId of the payload the button will send back
     * @param buttonStyle: default/primary/danger
     * @param warningText: the warning in the confirm dialog
     * @param confirmText: the text of the confirm button
     * @param denyText: the text of the deny button
     * @return button element with confirm dialog
     */
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

    /**
     * @param imageUrl: the url of the image
     * @param altText: the alt text of the image
     * @return an image as a block
     */
    public static LayoutBlock ImageBlock(String imageUrl, String altText) {
        return section(section ->
            section
                .text(markdownText(altText))
                .accessory(BlockElements.image(image -> image.imageUrl(imageUrl).altText(altText)))
        );
    }

    /**
     * @param text: output text
     * @return a text box as a block
     */
    public static LayoutBlock TextBlock(String text) {
        return section(section -> section.text(markdownText(text)));
    }

    /**
     * @param text: output text
     * @param imageUrl: the url of the image
     * @return a text + image box as a block
     */
    public static LayoutBlock TextImageBlock(String text, String imageUrl) {
        return section(section ->
            section
                .text(markdownText(text))
                .accessory(BlockElements.image(image -> image.imageUrl(imageUrl).altText(text)))
        );
    }
}
