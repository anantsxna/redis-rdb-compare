package org.example;

import com.slack.api.Slack;
import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SlackMain {
    private static final Logger logger = LogManager.getLogger(SlackMain.class);
    private static void postResponseAsync(String responseMessage, String channelId) {
        Slack slack = Slack.getInstance();
        String token = System.getenv("SLACK_BOT_TOKEN");
        slack.methodsAsync(token).chatPostMessage(req2 -> req2
                        .channel(channelId)
                        .text(responseMessage))
                .thenAcceptAsync(res -> {
                    if (res.isOk()) {
                        Message postedMessage = res.getMessage();
                    } else {
                        String errorCode = res.getError(); // e.g., "invalid_auth", "channel_not_found"
                        logger.error("Error posting message: " + errorCode);
                    }
                });
    }

    public static void main(String[] args) {
        System.out.println("Hello world, starting bot!");
        var app = new App();
        app.command("/ping", (req, ctx) -> {
            return ctx.ack(":wave: Pong");
        });
        app.command("/parse", (req, ctx) -> {
            postResponseAsync(SlackHelper.parseUtils(), req.getContext().getChannelId());
            return ctx.ack();
        });
        app.command("/maketrie", (req, ctx) -> {
            postResponseAsync(SlackHelper.trieConstructionUtils(), req.getContext().getChannelId());
            return ctx.ack();
        });
        app.command("/frequency", (req, ctx) -> {
            postResponseAsync(SlackHelper.countUtils(req.getPayload().getText()), req.getContext().getChannelId());
            return ctx.ack();
        });
        app.command("/getnext", (req, ctx) -> {
            postResponseAsync(SlackHelper.getNextKeyUtils(req.getPayload().getText()), req.getContext().getChannelId());
            return ctx.ack();
        });
        app.command("/redis-bot-help", (req, ctx) -> ctx.ack("""
                usage:
                \t*/ping*                                  - check if the bot if working.
                \t*/parse*                                - parse the input string and return the result. Input via S3 links not implemented yet.
                \t*/maketrie*                          - create the tries and store the parsed keys inside them. Requires \"/parse\" to be called first.
                \t*/frequency [prefix_key]*   - return the frequency of the prefix_key inside both the tries. Requires \"/maketrie\" to be called first.
                \t*/getnext [prefix_key] [n]*  - return the most common 'n' keys that have the same prefix, \'prefix_key\'. Requires \"/maketrie\" to be called first.
                """));
        try {
            new SocketModeApp(app).start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
