package org.messaging;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatUpdateResponse;
import com.slack.api.model.block.LayoutBlock;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for PostUpdate.
 * Contains methods posting, updating and deleting messages asynchronously and synchronously.
 *
 * Rigid design classes from PostUpdate invoke methods from this class to post, update and delete messages.
 */
@Slf4j
public class PostUpdateUtils {

    static Slack slack = Slack.getInstance();
    static String token = System.getenv("SLACK_BOT_TOKEN");

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
        log.info("Posting message synchronously in channel {}...", channelId);
        slack
            .methodsAsync(token)
            .chatPostMessage(req ->
                req.channel(channelId).text(responseMessage).blocks(layoutBlocks)
            )
            .thenAcceptAsync(res -> {
                if (res.isOk()) {
                    log.info("Message posted successfully.");
                } else {
                    String errorCode = res.getError(); // e.g., "invalid_auth", "channel_not_found"
                    log.info("Error posting message: {}", errorCode);
                }
            });
    }

    /**
     * Delete an existing message to a channel async.
     * @param channelId Channel to delete from.
     * @param timestamp The message to delete is determined by the timestamp parameter.
     */
    public static void deleteResponseAsync(final String channelId, String timestamp) {
        log.info("Deleting message asynchronously...");
        slack
            .methodsAsync(token)
            .chatDelete(req -> req.channel(channelId).ts(timestamp))
            .thenAcceptAsync(res -> {
                if (res.isOk()) {
                    log.info("Message deleted successfully.");
                } else {
                    String errorCode = res.getError(); // e.g., "invalid_auth", "channel_not_found"
                    log.info("Error deleting message: {}", errorCode);
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
        log.info("Updating message asynchronously in channel {}...", channelId);
        slack
            .methodsAsync(token)
            .chatUpdate(req ->
                req.channel(channelId).ts(timestamp).text(responseMessage).blocks(layoutBlocks)
            )
            .thenAcceptAsync(res -> {
                if (res.isOk()) {
                    log.info("Message updated successfully.");
                } else {
                    String errorCode = res.getError(); // e.g., "invalid_auth", "channel_not_found"
                    log.info("Error updating message: {}", errorCode);
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
        ChatUpdateResponse response = null;
        log.info("Updating message synchronously in channel {}...", channelId);
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
            log.info("Message updated successfully.");
        } else {
            String errorCode = response.getError(); // e.g., "invalid_auth", "channel_not_found"
            log.info("Error updating message: {}", errorCode);
        }
    }
}
