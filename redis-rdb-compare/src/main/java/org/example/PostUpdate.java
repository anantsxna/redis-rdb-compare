package org.example;

import static org.example.Blocks.*;
import static org.example.PostUpdateUtils.postResponseAsync;
import static org.example.PostUpdateUtils.updateResponseAsync;

import com.slack.api.model.block.LayoutBlock;
import java.util.ArrayList;
import java.util.List;

public class PostUpdate {

    private static final String REDIS_LOGO_URL =
        "https://avatars.githubusercontent.com/u/1529926?s=200&v=4";

    public static void postSimpleResponseAsync(String responseMessage, String channelId) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(TextImageBlock(responseMessage, REDIS_LOGO_URL));
        postResponseAsync(blocks, channelId, responseMessage);
    }

    public static void postResetButtonResponseAsync(String responseMessage, String channelId) {
        postSimpleButtonResponseAsync(
            responseMessage,
            "Reset Bot and Start Fresh?",
            "resetAll",
            "danger",
            channelId
        );
    }

    public static void postSimpleButtonResponseAsync(
        String responseMessage,
        String buttonText,
        String buttonValue,
        String buttonStyle,
        String channelId
    ) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(TextBlock(responseMessage));
        blocks.add(Blocks.ButtonBlock(buttonText, buttonValue, buttonStyle));
        postResponseAsync(blocks, channelId, responseMessage);
    }

    public static void updateSimpleResponseAsync(
        String responseMessage,
        String channelId,
        String timestamp
    ) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(TextImageBlock(responseMessage, REDIS_LOGO_URL));
        updateResponseAsync(blocks, channelId, responseMessage, timestamp);
    }
}
