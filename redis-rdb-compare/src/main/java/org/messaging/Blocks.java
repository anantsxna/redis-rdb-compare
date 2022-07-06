package org.messaging;

import static com.slack.api.model.block.Blocks.actions;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.button;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.ConfirmationDialogObject;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElements;
import com.slack.api.model.block.element.ButtonElement;

/**
 * Methods for specific blocks used in responses.
 */
public class Blocks {

    /**
     * @param buttonText:  the text of the button
     * @param buttonValue: the value used to make the actionId of the payload the button will send back
     * @param buttonStyle: default/primary/danger
     * @return a button
     */
    public static ButtonElement buttonElement(
        String buttonText,
        String buttonValue,
        String buttonStyle
    ) {
        return button(b ->
            b
                .text(plainText(buttonText))
                .style(buttonStyle)
                .value(buttonValue)
                .actionId("buttonBlock-" + buttonValue + "-" + randomAlphanumeric(10))
        );
    }

    /**
     * @param buttonText:  the text of the button
     * @param buttonValue: the value used to make the actionId of the payload the button will send back
     * @param buttonStyle: default/primary/danger
     * @param warningText: the warning in the confirm dialog
     * @param confirmText: the text of the confirm button
     * @param denyText:    the text of the deny button
     * @return a button with a confirm dialog
     */
    public static ButtonElement buttonWithConfirmationDialog(
        String buttonText,
        String buttonValue,
        String buttonStyle,
        String warningText,
        String confirmText,
        String denyText
    ) {
        return button(b ->
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
        );
    }

    /**
     * @param buttonText:  the text of the button
     * @param buttonValue: the value used to make the actionId of the payload the button will send back
     * @param buttonStyle: default/primary/danger
     * @return button block
     */
    public static LayoutBlock ButtonBlock(
        String buttonText,
        String buttonValue,
        String buttonStyle
    ) {
        return actions(actions ->
            actions.elements(asElements(buttonElement(buttonText, buttonValue, buttonStyle)))
        );
    }

    /**
     * @param buttonTextA:  the text of the first button
     * @param buttonValueA: the value used to make the actionId of the payload the first button will send back
     * @param buttonStyleA: default/primary/danger
     * @return an action block with 2 buttons
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
                    buttonElement(buttonTextA, buttonValueA, buttonStyleA),
                    buttonElement(buttonTextB, buttonValueB, buttonStyleB)
                )
            )
        );
    }

    /**
     * @param buttonTextA:  the text of the first button
     * @param buttonValueA: the value used to make the actionId of the payload the first button will send back
     * @param buttonStyleA: default/primary/danger
     * @param warningText:  the warning in the confirm dialog
     * @param confirmText:  the text of the confirm button
     * @param denyText:     the text of the deny button
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
                    buttonElement(buttonTextA, buttonValueA, buttonStyleA),
                    buttonElement(buttonTextB, buttonValueB, buttonStyleB),
                    buttonWithConfirmationDialog(
                        buttonTextC,
                        buttonValueC,
                        buttonStyleC,
                        warningText,
                        confirmText,
                        denyText
                    )
                )
            )
        );
    }

    /**
     * @param buttonTextA:  the text of the first button
     * @param buttonValueA: the value used to make the actionId of the payload the first button will send back
     * @param buttonStyleA: default/primary/danger
     * @return an action block with 4 buttons, the third button will open a confirm dialog
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
        String buttonStyleC
    ) {
        return actions(actions ->
            actions.elements(
                asElements(
                    buttonElement(buttonTextA, buttonValueA, buttonStyleA),
                    buttonElement(buttonTextB, buttonValueB, buttonStyleB),
                    buttonElement(buttonTextC, buttonValueC, buttonStyleC)
                )
            )
        );
    }

    /**
     * @param buttonText:  the text of the button
     * @param buttonValue: the value used to make the actionId of the payload the button will send back
     * @param buttonStyle: default/primary/danger
     * @param warningText: the warning in the confirm dialog
     * @param confirmText: the text of the confirm button
     * @param denyText:    the text of the deny button
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
                    buttonWithConfirmationDialog(
                        buttonText,
                        buttonValue,
                        buttonStyle,
                        warningText,
                        confirmText,
                        denyText
                    )
                )
            )
        );
    }

    /**
     * @param imageUrl: the url of the image
     * @param altText:  the alt text of the image
     * @return an image as a block
     */
    public static LayoutBlock ImageBlock(String imageUrl, String altText) {
        return section(section ->
            section
                .text(PlainTextObject.builder().text(altText).emoji(true).build())
                .accessory(BlockElements.image(image -> image.imageUrl(imageUrl).altText(altText)))
        );
    }

    /**
     * @param text: output text
     * @return a text box as a block
     */
    public static LayoutBlock TextBlock(String text) {
        return section(section -> section.text(MarkdownTextObject.builder().text(text).build()));
    }

    /**
     * @param text:     output text
     * @param imageUrl: the url of the image
     * @return a text + image box as a block
     */
    public static LayoutBlock TextImageBlock(String text, String imageUrl) {
        return section(section ->
            section
                .text(MarkdownTextObject.builder().text(text).build())
                .accessory(BlockElements.image(image -> image.imageUrl(imageUrl).altText(text)))
        );
    }
}
