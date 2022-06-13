package org.messaging;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatUpdateResponse;
import com.slack.api.model.block.LayoutBlock;
import java.io.IOException;
import java.util.List;

/**
 * Utility class for PostUpdate.
 * Contains methods posting, updating and deleting messages asynchronously and synchronously.
 *
 * Rigid design classes from PostUpdate invoke methods from this class to post, update and delete messages.
 */
public class PostUpdateUtils {

    /**
     * Post a new message to a channel async.
     * @param layoutBlocks List of blocks that make the message body.
     * @param channelId Channel to post to.
     * @param responseMessage Message to post.
     */
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

    /**
     * Delete an existing message to a channel async.
     * @param channelId Channel to delete from.
     * @param timestamp The message to delete is determined by the timestamp parameter.
     */
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

    /**
     * Update an existing message to a channel async.
     * @param layoutBlocks List of blocks that make the message body.
     * @param channelId Channel to update.
     * @param responseMessage Message text to update.
     * @param timestamp The message to update is determined by the timestamp parameter.
     */
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

    /**
     * Post a new message to a channel sync.
     * @param layoutBlocks List of blocks that make the message body.
     * @param channelId Channel to post to.
     * @param responseMessage Message to post.
     * @param timestamp The message to post is determined by the timestamp parameter.
     */
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
