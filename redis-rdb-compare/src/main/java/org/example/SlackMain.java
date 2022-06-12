package org.example;

import static org.example.PostUpdate.*;
import static org.example.SlackUtils.countUtils;
import static org.example.SlackUtils.parseUtils;
import static org.example.SlackUtils.trieConstructionUtils;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.util.URLDecoder;

public class SlackMain {

    private static final Logger logger = LogManager.getLogger(SlackMain.class);

    public static void main(String[] args) {
        System.out.println("Hello world, starting bot!");
        var app = new App();
        app.command(
            "/ping",
            (req, ctx) -> {
                return ctx.ack(":wave: Pong");
            }
        );
        app.command(
            "/parse",
            (req, ctx) -> {
                String channelId = req.getContext().getChannelId();
                String response = parseUtils(channelId);
                if (response.equals(SlackUtils.PARSING_COMPLETED)) {
                    postResetButtonResponseAsync(response, channelId);
                } else {
                    postSimpleResponseAsync(response, req.getContext().getChannelId());
                }
                return ctx.ack();
            }
        );
        app.command(
            "/maketrie",
            (req, ctx) -> {
                String channelId = req.getContext().getChannelId();
                postSimpleResponseAsync(trieConstructionUtils(channelId), channelId);
                return ctx.ack();
            }
        );
        app.command(
            "/frequency",
            (req, ctx) -> {
                String channelId = req.getContext().getChannelId();
                postSimpleResponseAsync(
                    countUtils(req.getPayload().getText(), channelId),
                    channelId
                );
                return ctx.ack();
            }
        );
        app.command(
            "/getnext",
            (req, ctx) -> {
                String channelId = req.getContext().getChannelId();
                postSimpleResponseAsync(
                    SlackUtils.getNextKeyUtils(req.getPayload().getText(), channelId),
                    channelId
                );
                return ctx.ack();
            }
        );
        app.command(
            "/interact",
            (req, ctx) -> {
                return ctx.ack();
            }
        );
        app.command(
            "/redis-bot-help",
            (req, ctx) ->
                ctx.ack(
                    """
                            usage:
                            \t*/ping*                                  - check if the bot if working.
                            \t*/parse*                                - parse the input string and return the result. Input via S3 links not implemented yet.
                            \t*/maketrie*                          - create the tries and store the parsed keys inside them. Requires "/parse" to be called first.
                            \t*/frequency [prefix_key]*   - return the frequency of the prefix_key inside both the tries. Requires "/maketrie" to be called first.
                            \t*/getnext [prefix_key] [n]*  - return the most common 'n' keys that have the same prefix, 'prefix_key'. Requires "/maketrie" to be called first.
                            """
                )
        );
        app.blockAction(
            Pattern.compile("^buttonBlock-resetAll-\\d*"),
            (req, ctx) -> {
                String actionId = req.getPayload().getActions().get(0).getActionId();
                assert (actionId.startsWith("buttonBlock-resetAll-"));
                String messageTs = req.getPayload().getContainer().getMessageTs();
                String channelId = req.getPayload().getChannel().getId();
                //TODO: Switch to complex response with new start page
                updateSimpleResponseAsync("You clicked the reset button", channelId, messageTs);
                return ctx.ack();
            }
        );

        try {
            SocketModeApp socketModeApp = new SocketModeApp(app);
            socketModeApp.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
