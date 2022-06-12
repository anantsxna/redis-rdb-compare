package org.example;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatUpdateResponse;
import com.slack.api.model.block.LayoutBlock;
import java.io.IOException;
import java.util.List;

public class PostUpdateUtils {

    public static void postResponseAsync(
        List<LayoutBlock> layoutBlocks,
        final String channelId,
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

    public static void deleteResponseAsync(final String channelId, String timestamp) {
        Slack slack = Slack.getInstance();
        String token = System.getenv("SLACK_BOT_TOKEN");
        slack
            .methodsAsync(token)
            .chatDelete(req -> req.channel(channelId).ts(timestamp))
            .thenAcceptAsync(res -> {
                if (res.isOk()) {
                    System.out.println("Message deleted successfully.");
                } else {
                    String errorCode = res.getError(); // e.g., "invalid_auth", "channel_not_found"
                    System.out.println("Error deleting message: " + errorCode);
                    throw new RuntimeException("Error deleting message: " + errorCode);
                }
            });
    }

    public static void updateResponseAsync(
        List<LayoutBlock> layoutBlocks,
        final String channelId,
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

    public static void updateResponseSync(
        List<LayoutBlock> layoutBlocks,
        final String channelId,
        String responseMessage,
        String timestamp
    ) {
        Slack slack = Slack.getInstance();
        String token = System.getenv("SLACK_BOT_TOKEN");
        ChatUpdateResponse response = null;
        System.out.println("Updating message synchronously...");
        try {
            response =
                slack
                    .methods(token)
                    .chatUpdate(req ->
                        req
                            .channel(channelId)
                            .ts(timestamp)
                            .text(responseMessage)
                            .blocks(layoutBlocks)
                    );
        } catch (IOException | SlackApiException e) {
            throw new RuntimeException(e);
        }

        if (response.isOk()) {
            System.out.println("Message updated successfully.");
        } else {
            String errorCode = response.getError(); // e.g., "invalid_auth", "channel_not_found"
            System.out.println("Error updating message: " + errorCode);
            throw new RuntimeException("Error updating message: " + errorCode);
        }
    }
}
