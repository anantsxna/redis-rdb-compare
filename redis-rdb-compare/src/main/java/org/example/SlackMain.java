package org.example;

import static org.example.SlackUtils.*;
import static org.messaging.PostUpdate.*;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageChangedEvent;
import com.slack.api.model.event.MessageDeletedEvent;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.util.thread.DaemonThreadExecutorServiceProvider;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.messaging.ProcessView;

/**
 * Slack App that handles the following:
 * - Slash Commands (/ping, /parse, /maketrie, /reset, /redis-bot-help, /start)
 * - Events from Slack (AppMentionEvent, MessageChangedEvent, MessageDeletedEvent)
 * - Interactive components' payloads to the App via blockActions
 * <p>
 * This class is the main entry point for the application.
 * Each botSession within the Slack workspace is treated as a separate client
 */
@Slf4j
public class SlackMain {

    private static final String SESSION_IN_PROGRESS =
        "A session is already open in this botSession.\n";
    private static final String QUERYING_NOT_POSSIBLE =
        "Querying is not possible since tries have not been created.\n";

    /**
     * Main method for the application.
     */
    public static void main(String[] args) {
        var app = new App(
            AppConfig
                .builder()
                .executorServiceProvider(DaemonThreadExecutorServiceProvider.getInstance())
                .signingSecret(System.getenv("SLACK_SIGNING_SECRET"))
                .threadPoolSize(15)
                .build()
        );

        // RE: command "/ping" - responds with "pong"
        app.command(
            "/ping",
            (req, ctx) -> {
                log.info("/ping command received");
                return ctx.ack(":wave: Pong");
            }
        );

        // RE: command "/session" - opens a new bot session
        app.command(
            "/session",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("/session command received");
                        final String channelId = req.getContext().getChannelId();
                        final String response = createSessionUtils();
                        log.info("/session command response:\n" + response);
                        postTextResponseAsync(response, channelId);
                    });
                return ctx.ack();
            }
        );

        // RE: command "/process" - opens a new bot session, downloads, parses, and makes tries.
        app.command(
            "/process",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("/process command received");
                        final String channelId = req.getContext().getChannelId();
                        final String text = req.getPayload().getText();
                        String response = processAllUtils(text, channelId);
                        log.info("/process command response:\n" + response);
                        postTextResponseAsync(response, channelId);
                    });
                return ctx.ack();
            }
        );

        // RE: command "/download [requestId]" - starts parsing
        app.command(
            "/download",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        final String channelId = req.getContext().getChannelId();
                        final String text = req.getPayload().getText();
                        log.info("/download command received with arguments: {}", text);
                        final String response = downloadUtils(text, channelId, false);
                        log.info("downloadUtils response: {} in channelId {}", response, channelId);
                        postTextResponseAsync(response, channelId);
                    });
                return ctx.ack();
            }
        );

        // RE: command "/parse [requestId]" - starts parsing
        app.command(
            "/parse",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        final String channelId = req.getContext().getChannelId();
                        final String requestId = req.getPayload().getText();
                        log.info("/parse command received");
                        String response = parseUtils(requestId, channelId, false);
                        log.info("parseUtils response: {} in botSession {}", response, channelId);
                        postTextResponseAsync(response, channelId);
                    });
                return ctx.ack();
            }
        );

        // command "/maketrie [requestId]" - starts creating tries
        app.command(
            "/maketrie",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("/maketrie command received");
                        final String channelId = req.getContext().getChannelId();
                        final String requestId = req.getPayload().getText();
                        String response = makeTrieUtils(requestId, channelId, false);
                        log.info(
                            "makeTrieUtils() response: {} in botSession {}",
                            response,
                            channelId
                        );
                        postTextResponseAsync(response, channelId);
                    });
                return ctx.ack();
            }
        );

        // RE: command "/getcount [requestId] [prefixKey]" - gets the count of the prefixKey in all tries
        app.command(
            "/getcount",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("/getcount command received");
                        final String channelId = req.getContext().getChannelId();
                        final String text = req.getPayload().getText();
                        String response = countUtils(text, channelId);
                        log.info("countUtils response:\n {}", response);
                        postTextResponseAsync(response, channelId);
                    });
                return ctx.ack();
            }
        );

        // RE: command "/getnext [prefixKey] [n]" - gets 'n' keys with the prefixKey in all tries
        app.command(
            "/getnext",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("/getnext command received");
                        final String channelId = req.getContext().getChannelId();
                        final String text = req.getPayload().getText();
                        String response = getNextKeyUtils(text, channelId);
                        log.info("getNextKeyUtils response:\n {}", response);
                        postTextResponseAsync(response, channelId);
                    });
                return ctx.ack();
            }
        );

        // RE: command "/clear" - resets the sessions with the given requestId
        app.command(
            "/clear",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("/clear command received");
                        final String channelId = req.getContext().getChannelId();
                        String requestId = req.getPayload().getText();
                        String response = deleteSessionUtils(requestId);
                        log.info("deleteSessionUtils response: {}", response);
                        postTextResponseAsync(response, channelId);
                    });
                return ctx.ack();
            }
        );

        // RE: command "/clearall" - resets the data for all sessions
        app.command(
            "/clearall",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("/clearall command received");
                        final String channelId = req.getContext().getChannelId();
                        String response = deleteAllSessionsUtils();
                        log.info("deleteAllSessionsUtils response: {}", response);
                        postTextResponseAsync(response, channelId);
                    });
                return ctx.ack();
            }
        );

        // RE: command "/list" - lists all active sessions
        app.command(
            "/list",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("/list command received");
                        final String channelId = req.getContext().getChannelId();
                        String response = listSessionsUtils();
                        log.info("listSessionsUtils response: {}", response);
                        postTextResponseAsync(response, channelId);
                    });
                return ctx.ack();
            }
        );

        // RE: command "/redis-bot-help" - provides help for the bot
        app.command(
            "/redis-bot-help",
            (req, ctx) -> {
                log.info("/redis-bot-help command received");
                return ctx.ack(
                    """
                                    usage:
                                                                        \\t*/ping* - check if the bot if working.
                                                                        \\t*/start* - start interactive session.
                                                                        \\t*/clear [requestId]* - clear all files related to the session.
                                                                        \\t*/clearall* - clear all files related to all sessions.
                                                                        \\t*/list* - list all active sessions.
                                                                        \\t*/session* - start a new session, return a requestId.
                                                                        \\t*/download [requestId] [s3linkA] [s3linkB]* - download files from S3links to the session.
                                                                        \\t*/parse [requestId]* - parse the input string and return the result.\s
                                                                        \\t*/maketrie [requestId]* - create the tries and store the parsed keys inside them. Requires "/parse" to be called first.
                                                                        \\t*/getcount [requestId] [prefix_key]* - return the count of the prefix_key inside both the tries. Requires "/maketrie" to be called first.
                                                                        \\t*/getnext [requestId] [prefix_key] [n]* - return the most common 'n' keys that have the same prefix, 'prefix_key'. Requires "/maketrie" to be called first.
                                                                        """
                );
            }
        );

        // command "/start" - starts interactive session
        app.command(
            "/start",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("/start command received");
                        final String channelId = req.getContext().getChannelId();
                        String response = createSessionUtils();
                        if (response.equals(SESSION_IN_PROGRESS)) {
                            response = "A session is already open in this botSession.";
                            postResetButtonResponseAsync(response, channelId);
                        } else {
                            response = ":wave: Welcome to the interactive session.";
                            postStartButtonResponse(response, channelId);
                        }
                    });
                return ctx.ack();
            }
        );

        // blockActions - handle the interactive sessions' components' payloads
        // blockAction "parseAndMakeTrieAll" - handles the payload from the "Parse and Make Tries" button
        app.blockAction(
            Pattern.compile("^buttonBlock-parseAndMakeTrieAll-\\w*"),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("\"Parse and Make Tries\" button clicked");
                        final String channelId = req.getPayload().getChannel().getId();
                        final String messageTs = req.getPayload().getContainer().getMessageTs();
                        ProcessView processView = ProcessView
                            .builder()
                            .timestamp(messageTs)
                            .requestId(channelId)
                            .build();
                        new Thread(processView::run).start();
                    });
                return ctx.ack();
            }
        );

        // blockAction "queryAll" - handles the payload from the "Get Next" and "Get Count" buttons
        app.blockAction(
            Pattern.compile("^buttonBlock-queryAll-[-\\w]*"),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("Query button clicked");
                        final String actionId = req.getPayload().getActions().get(0).getActionId();
                        final String messageTs = req.getPayload().getContainer().getMessageTs();
                        final String channelId = req.getPayload().getChannel().getId();
                        final String response = queryAllUtils(channelId);
                        if (response.equals(QUERYING_NOT_POSSIBLE)) {
                            updateResetButtonResponseAsync(response, channelId, messageTs);
                        } else {
                            if (actionId.startsWith("buttonBlock-queryAll-count")) {
                                updateQueryCountResponseAsync(response, channelId, messageTs);
                            } else {
                                updateQueryNextResponseAsync(response, channelId, messageTs);
                            }
                        }
                    });
                return ctx.ack();
            }
        );

        // blockAction "resetAll" - handles the payload from the "Reset" button
        app.blockAction(
            Pattern.compile("^buttonBlock-resetAll-\\w*"),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("Reset button clicked");
                        final String messageTs = req.getPayload().getContainer().getMessageTs();
                        final String channelId = req.getPayload().getChannel().getId();
                        final String information = resetSessionUtils(channelId);
                        final String response = ":wave: Welcome to the interactive session.";
                        updateStartButtonResponse(information, response, channelId, messageTs);
                    });
                return ctx.ack();
            }
        );

        // blockAction "exitAll" - handles the payload from the "Close" button
        app.blockAction(
            Pattern.compile("^buttonBlock-exitAll-\\w*"),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("Close button clicked");
                        final String messageTs = req.getPayload().getContainer().getMessageTs();
                        final String channelId = req.getPayload().getChannel().getId();
                        deleteSessionUtils(channelId);
                        deleteStartButtonResponse(channelId, messageTs);
                    });
                return ctx.ack();
            }
        );

        // blockAction "countQuery" - handles the payload from the "Count of a Key" input
        app.blockAction(
            Pattern.compile("^inputBlock-countQuery-\\w*"),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("Count of a Key input received");
                        final String messageTs = req.getPayload().getContainer().getMessageTs();
                        final String channelId = req.getPayload().getChannel().getId();
                        final String prefixKey = req.getPayload().getActions().get(0).getValue();
                        final String response = countUtils(prefixKey, channelId);
                        updateQueryCountResponseAsync(response, channelId, messageTs);
                    });
                return ctx.ack();
            }
        );

        // blockAction "nextQuery" - handles the payload from the "Next Keys" input
        app.blockAction(
            Pattern.compile("^inputBlock-nextQuery-\\w*"),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("Next Keys input received");
                        final String messageTs = req.getPayload().getContainer().getMessageTs();
                        final String channelId = req.getPayload().getChannel().getId();
                        final String prefixKey_count = req
                            .getPayload()
                            .getActions()
                            .get(0)
                            .getValue();
                        final String response = getNextKeyUtils(prefixKey_count, channelId);
                        updateQueryNextResponseAsync(response, channelId, messageTs);
                    });
                return ctx.ack();
            }
        );

        // events - handle the interactive sessions' event based payloads
        app.event(MessageChangedEvent.class, (payload, ctx) -> ctx.ack());
        app.event(MessageEvent.class, (payload, ctx) -> ctx.ack());
        app.event(MessageDeletedEvent.class, (payload, ctx) -> ctx.ack());

        // event "AppMentionEvent" - starts the session when bot is @mentioned in a botSession
        app.event(
            AppMentionEvent.class,
            (payload, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("AppMentionEvent received");
                        final String channelId = payload.getEvent().getChannel();
                        String response = createSessionUtils();
                        if (response.equals(SESSION_IN_PROGRESS)) {
                            response = "A session is already open in this botSession.";
                            postResetButtonResponseAsync(response, channelId);
                        } else {
                            response = ":wave: Welcome to the interactive session.";
                            postStartButtonResponse(response, channelId);
                        }
                    });
                return ctx.ack();
            }
        );

        //cleanup shutdown hooks
        Runtime
            .getRuntime()
            .addShutdownHook(
                new Thread(() -> {
                    log.info("Shutting down bot...");
                    deleteAllSessionsUtils();
                    app.executorService().shutdown();
                    try {
                        boolean exit0 = app
                            .executorService()
                            .awaitTermination(10, TimeUnit.SECONDS);
                        assert !exit0 : "Executor service did not terminate within 10 seconds";
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
            );

        //start the app
        try {
            SocketModeApp socketModeApp = new SocketModeApp(app);
            log.info("App started. Hello World.");
            socketModeApp.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
