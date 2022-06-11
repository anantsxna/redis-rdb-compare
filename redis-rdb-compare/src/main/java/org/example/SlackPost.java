package org.example;

import static org.example.SlackPostUtils.*;

import com.slack.api.model.block.LayoutBlock;
import java.util.ArrayList;
import java.util.List;

public class SlackPost {

    private static final String REDIS_LOGO_URL =
        "https://avatars.githubusercontent.com/u/1529926?s=200&v=4";

    public static void postSimpleResponseAsync(String responseMessage, String channelId) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(TextImageBlock(responseMessage, REDIS_LOGO_URL));
        SlackPostUtils.postResponseAsync(blocks, channelId, responseMessage);
    }

    public static void postSimpleDangerButtonResponseAsync(
        String responseMessage,
        String buttonText,
        String buttonValue,
        String channelId
    ) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(TextBlock(responseMessage));
        blocks.add(SlackPostUtils.ButtonBlock(buttonText, buttonValue, "danger"));
        SlackPostUtils.postResponseAsync(blocks, channelId, responseMessage);
    }
}
