package org.example;

import static org.example.SlackUtils.*;
import static org.messaging.PostUpdate.*;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageChangedEvent;
import com.slack.api.model.event.MessageDeletedEvent;
import com.slack.api.model.event.MessageEvent;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.messaging.ParseAndMakeTrieView;

/**
 * Slack App that handles the following:
 * - Slash Commands (/ping, /parse, /maketrie, /reset, /redis-bot-help, /start)
 * - Events from Slack (AppMentionEvent, MessageChangedEvent, MessageDeletedEvent)
 * - Interactive components' payloads to the App via blockActions
 *
 * This class is the main entry point for the application.
 * Each channel within the Slack workspace is treated as a separate client
 */
public class SlackMain {

    private static final Logger logger = LogManager.getLogger(SlackMain.class);
    private static final String SESSION_IN_PROGRESS =
        "A session is already open in this channel.\n";
    private static final String PARSING_COMPLETED = "Parsing completed.";
    private static final String QUERYING_NOT_POSSIBLE =
        "Querying is not possible since tries have not been created.\n";

    /**
     * Main method for the application.
     */
    public static void main(String[] args) {
        System.out.println("Hello world, starting bot!");
        var app = new App();
        // command "/ping" - responds with "pong"
        app.command(
            "/ping",
            (req, ctx) -> {
                return ctx.ack(":wave: Pong");
            }
        );

        // command "/parse" - starts parsing
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

        // command "/maketrie" - starts creating tries
        app.command(
            "/maketrie",
            (req, ctx) -> {
                final String channelId = req.getContext().getChannelId();
                postTextResponseAsync(trieConstructionUtils(channelId), channelId);
                return ctx.ack();
            }
        );

        // command "/getcount [prefixKey]" - gets the count of the prefixKey in all tries
        app.command(
            "/getcount",
            (req, ctx) -> {
                final String channelId = req.getContext().getChannelId();
                postTextResponseAsync(countUtils(req.getPayload().getText(), channelId), channelId);
                return ctx.ack();
            }
        );

        // command "/getnext [prefixKey] [n]" - gets 'n' keys with the prefixKey in all tries
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

        // command "/clear" - resets the data within the channel
        app.command(
            "/clear",
            (req, ctx) -> {
                final String channelId = req.getContext().getChannelId();
                postTextResponseAsync(clearUtils(channelId), channelId);
                return ctx.ack();
            }
        );

        // command "/redis-bot-help" - provides help for the bot
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

        // command "/start" - starts interactive session
        app.command(
            "/start",
            (req, ctx) -> {
                final String channelId = req.getContext().getChannelId();
                String response = "Welcome to the interactive session.";
                postStartButtonResponse(response, channelId);
                return ctx.ack();
            }
        );

        // blockActions - handle the interactive sessions' components' payloads
        // blockAction "startAll" - handles the payload from the "Parse and Make Tries" button
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

        // blockAction "queryAll" - handles the payload from the "Get Next" and "Get Count" buttons
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

        // blockAction "resetAll" - handles the payload from the "Reset" button
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

        // blockAction "exitAll" - handles the payload from the "Close" button
        app.blockAction(
            Pattern.compile("^buttonBlock-exitAll-\\w*"),
            (req, ctx) -> {
                String messageTs = req.getPayload().getContainer().getMessageTs();
                final String channelId = req.getPayload().getChannel().getId();
                deleteStartButtonResponse(channelId, messageTs);
                return ctx.ack();
            }
        );

        // blockAction "countQuery" - handles the payload from the "Count of a Key" input
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

        // blockAction "nextQuery" - handles the payload from the "Next Keys" input
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

        // events - handle the interactive sessions' event based payloads
        app.event(MessageChangedEvent.class, (payload, ctx) -> ctx.ack());
        app.event(MessageEvent.class, (payload, ctx) -> ctx.ack());
        app.event(MessageDeletedEvent.class, (payload, ctx) -> ctx.ack());
        // event "AppMentionEvent" - starts the session when bot is @mentioned in a channel
        app.event(
            AppMentionEvent.class,
            (payload, ctx) -> {
                final String channelId = payload.getEvent().getChannel();
                String response = "Welcome to the interactive session.";
                postStartButtonResponse(response, channelId);
                return ctx.ack();
            }
        );

        //start the app
        try {
            SocketModeApp socketModeApp = new SocketModeApp(app);
            socketModeApp.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
