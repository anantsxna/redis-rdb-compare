package org.example;

import static org.example.PostUpdate.*;
import static org.example.SlackUtils.*;

import com.google.gson.Gson;
import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.MessageChangedEvent;
import com.slack.api.model.event.MessageDeletedEvent;
import com.slack.api.model.event.MessageEvent;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SlackMain {

    private static final Logger logger = LogManager.getLogger(SlackMain.class);
    private static final String SESSION_IN_PROGRESS =
        "A session is already open in this channel.\n";
    private static final String PARSING_COMPLETED = "Parsing completed.";
    private static final String QUERYING_NOT_POSSIBLE =
        "Querying is not possible since tries have not been created.\n";

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
                final String channelId = req.getContext().getChannelId();
                String response = parseUtils(channelId);
                if (response.equals(PARSING_COMPLETED)) {
                    postResetButtonResponseAsync(response, channelId);
                } else {
                    postTextResponseAsync(response, channelId);
                }
                return ctx.ack();
            }
        );
        app.command(
            "/maketrie",
            (req, ctx) -> {
                final String channelId = req.getContext().getChannelId();
                postTextResponseAsync(trieConstructionUtils(channelId), channelId);
                return ctx.ack();
            }
        );
        app.command(
            "/getcount",
            (req, ctx) -> {
                final String channelId = req.getContext().getChannelId();
                postTextResponseAsync(countUtils(req.getPayload().getText(), channelId), channelId);
                return ctx.ack();
            }
        );
        app.command(
            "/getnext",
            (req, ctx) -> {
                final String channelId = req.getContext().getChannelId();
                postTextResponseAsync(
                    SlackUtils.getNextKeyUtils(req.getPayload().getText(), channelId),
                    channelId
                );
                return ctx.ack();
            }
        );

        app.command(
            "/clear",
            (req, ctx) -> {
                final String channelId = req.getContext().getChannelId();
                postTextResponseAsync(clearUtils(channelId), channelId);
                return ctx.ack();
            }
        );

        app.command(
            "/redis-bot-help",
            (req, ctx) ->
                ctx.ack(
                    """
                            usage:
                            \t*/ping* - check if the bot if working.
                            \t*/start* - start interactive session.
                            \t*/clear* - clear all files related to the current session.
                            \t*/parse* - parse the input string and return the result. Input via S3 links not implemented yet.
                            \t*/maketrie* - create the tries and store the parsed keys inside them. Requires "/parse" to be called first.
                            \t*/getcount [prefix_key]* - return the count of the prefix_key inside both the tries. Requires "/maketrie" to be called first.
                            \t*/getnext [prefix_key] [n]* - return the most common 'n' keys that have the same prefix, 'prefix_key'. Requires "/maketrie" to be called first.
                            """
                )
        );

        app.command(
            "/start",
            (req, ctx) -> {
                final String channelId = req.getContext().getChannelId();
                String response = "Welcome to the interactive session.";
                postStartButtonResponse(response, channelId);
                return ctx.ack();
            }
        );

        app.blockAction(
            Pattern.compile("^buttonBlock-startAll-\\w*"),
            (req, ctx) -> {
                final String channelId = req.getPayload().getChannel().getId();
                String response = startAllUtils(channelId);
                String messageTs = req.getPayload().getContainer().getMessageTs();
                if (response.equals(SESSION_IN_PROGRESS)) {
                    System.out.println("Session in progress");
                    updateResetButtonResponseAsync(response, channelId, messageTs);
                } else {
                    System.out.println("Session not in progress");
                    ParseAndMakeTrieView parseAndMakeTrieView = ParseAndMakeTrieView
                        .builder()
                        .timestamp(messageTs)
                        .channelId(channelId)
                        .build();
                    new Thread(parseAndMakeTrieView::run).start();
                }
                return ctx.ack();
            }
        );

        app.blockAction(
            Pattern.compile("^buttonBlock-queryAll-[-\\w]*"),
            (req, ctx) -> {
                String actionId = req.getPayload().getActions().get(0).getActionId();
                String messageTs = req.getPayload().getContainer().getMessageTs();
                final String channelId = req.getPayload().getChannel().getId();
                String response = queryAllUtils(channelId);
                if (response.equals(QUERYING_NOT_POSSIBLE)) {
                    updateResetButtonResponseAsync(response, channelId, messageTs);
                } else {
                    if (actionId.startsWith("buttonBlock-queryAll-count")) {
                        updateQueryCountResponseAsync(response, channelId, messageTs);
                    } else {
                        updateQueryNextResponseAsync(response, channelId, messageTs);
                    }
                }
                return ctx.ack();
            }
        );

        app.blockAction(
            Pattern.compile("^buttonBlock-resetAll-\\w*"),
            (req, ctx) -> {
                String messageTs = req.getPayload().getContainer().getMessageTs();
                final String channelId = req.getPayload().getChannel().getId();
                clearUtils(channelId);
                String information = "Deleted: Bot files for this channel.";
                String response = "Welcome to the new interactive session.";
                updateStartButtonResponse(information, response, channelId, messageTs);
                return ctx.ack();
            }
        );

        app.blockAction(
            Pattern.compile("^buttonBlock-exitAll-\\w*"),
            (req, ctx) -> {
                String messageTs = req.getPayload().getContainer().getMessageTs();
                final String channelId = req.getPayload().getChannel().getId();
                deleteStartButtonResponse(channelId, messageTs);
                return ctx.ack();
            }
        );

        app.blockAction(
            Pattern.compile("^inputBlock-countQuery-\\w*"),
            (req, ctx) -> {
                String messageTs = req.getPayload().getContainer().getMessageTs();
                final String channelId = req.getPayload().getChannel().getId();
                String prefixKey = req.getPayload().getActions().get(0).getValue();
                String response = countUtils(prefixKey, channelId);
                updateQueryCountResponseAsync(response, channelId, messageTs);
                return ctx.ack();
            }
        );

        app.blockAction(
            Pattern.compile("^inputBlock-nextQuery-\\w*"),
            (req, ctx) -> {
                String messageTs = req.getPayload().getContainer().getMessageTs();
                final String channelId = req.getPayload().getChannel().getId();
                String prefixKey_count = req.getPayload().getActions().get(0).getValue();
                String response = getNextKeyUtils(prefixKey_count, channelId);
                updateQueryNextResponseAsync(response, channelId, messageTs);
                return ctx.ack();
            }
        );

        app.event(MessageChangedEvent.class, (payload, ctx) -> ctx.ack());
        app.event(MessageEvent.class, (payload, ctx) -> ctx.ack());
        app.event(MessageDeletedEvent.class, (payload, ctx) -> ctx.ack());

        try {
            SocketModeApp socketModeApp = new SocketModeApp(app);
            socketModeApp.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
