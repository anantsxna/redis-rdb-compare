package org.example;

import com.slack.api.Slack;
import com.slack.api.model.block.LayoutBlock;
import java.util.List;

public class PostUpdateUtils {

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

    public static void updateResponseAsync(
        List<LayoutBlock> layoutBlocks,
        String channelId,
        String responseMessage,
        String timestamp
    ) {
        Slack slack = Slack.getInstance();
        String token = System.getenv("SLACK_BOT_TOKEN");
        slack
            .methodsAsync(token)
            .chatUpdate(req ->
                req.channel(channelId).ts(timestamp).text(responseMessage).blocks(layoutBlocks)
            )
            .thenAcceptAsync(res -> {
                if (res.isOk()) {
                    System.out.println("Message updated successfully.");
                } else {
                    String errorCode = res.getError(); // e.g., "invalid_auth", "channel_not_found"
                    System.out.println("Error updating message: " + errorCode);
                    throw new RuntimeException("Error updating message: " + errorCode);
                }
            });
    }
}
