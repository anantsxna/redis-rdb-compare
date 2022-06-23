package org.example;

import static org.example.SlackUtils.*;
import static org.messaging.PostUpdate.postTextResponseAsync;
import static org.messaging.PostUpdateUtils.deleteResponseAsync;

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
import org.views.MenuView;
import org.views.ProcessView;
import org.views.QueryView;

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
public class Main {

    /**
     * Main method for the application.
     */
    public static void main(String[] args) {
        log.info(System.getenv("SLACK_SIGNING_SECRET"));
        log.info(System.getenv("SLACK_BOT_TOKEN"));
        log.info(System.getenv("SLACK_APP_TOKEN"));
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
                        String response = countUtils(text);
                        log.info("countUtils response:\n {}", response);
                        postTextResponseAsync(response, channelId);
                    });
                return ctx.ack();
            }
        );

        // RE: command "/getnext [requestId] [prefixKey] [n]" - gets 'n' keys with the prefixKey in all tries
        app.command(
            "/getnext",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("/getnext command received");
                        final String channelId = req.getContext().getChannelId();
                        final String text = req.getPayload().getText();
                        String response = getNextKeyUtils(text);
                        log.info("getNextKeyUtils response:\n {}", response);
                        postTextResponseAsync(response, channelId);
                    });
                return ctx.ack();
            }
        );

        // RE: command "/clear [requestId]" - resets the sessions with the given requestId
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
                                            */ping* 
                                                    - check if the bot if working.
                                            */menu* 
                                                    - start interactive session.
                                            */clear [requestId]* 
                                                    - clear all files related to the session.
                                            */clearall* 
                                                    - clear all files related to all sessions.
                                            */list* 
                                                    - list all active sessions.
                                            */session* 
                                                    - start a new session, return a requestId.
                                            */download [requestId] [s3linkA] [s3linkB]* 
                                                    - download files from S3links to the session.
                                            */parse [requestId]* 
                                                    - parse the input string and return the result.\s
                                            */maketrie [requestId]*
                                                    - create the tries and store the parsed keys inside them. Requires "/parse" to be called first.
                                            */getcount [requestId] [prefix_key]* 
                                                    - return the count of the prefix_key inside both the tries. Requires "/maketrie" to be called first.
                                            */getnext [requestId] [prefix_key] [n]* 
                                                    - return the most common 'n' keys that have the same prefix, 'prefix_key'. Requires "/maketrie" to be called first.
                                                                        """
                );
            }
        );

        // command "/menu" - starts interactive session
        app.command(
            "/menu",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("/menu command received");
                        final String channelId = req.getContext().getChannelId();
                        MenuView menuView = MenuView
                            .builder()
                            .channelId(channelId)
                            .messageTs(null)
                            .build();
                        menuView.start();
                    });
                return ctx.ack();
            }
        );

        // blockAction - clicked 'Close' button
        app.blockAction(
            Pattern.compile("^buttonBlock-delete-message-\\w*"),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("\"Close\" button clicked on HomeView");
                        final String channelId = req.getPayload().getChannel().getId();
                        final String messageTs = req.getPayload().getContainer().getMessageTs();
                        deleteResponseAsync(channelId, messageTs);
                    });
                return ctx.ack();
            }
        );

        // blockAction - clicked 'Delete Session' button
        app.blockAction(
            Pattern.compile("^buttonBlock-delete-session-\\w*"),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("\"Close\" button clicked on HomeView");
                        final String channelId = req.getPayload().getChannel().getId();
                        final String messageTs = req.getPayload().getContainer().getMessageTs();
                        final String requestId = req.getPayload().getMessage().getText();
                        deleteSessionUtils(requestId);
                        MenuView menuView = MenuView
                            .builder()
                            .channelId(channelId)
                            .messageTs(messageTs)
                            .build();
                        menuView.start();
                    });
                return ctx.ack();
            }
        );

        // blockAction - clicked 'New Session' button
        app.blockAction(
            Pattern.compile("^buttonBlock-create-new-session-\\w*"),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("In Main, \"Create New\" button clicked on HomeView");
                        final String channelId = req.getPayload().getChannel().getId();
                        final String messageTs = req.getPayload().getContainer().getMessageTs();
                        MenuView menuView = MenuView
                            .builder()
                            .channelId(channelId)
                            .messageTs(messageTs)
                            .build();
                        menuView.start();
                    });
                return ctx.ack();
            }
        );

        // blockAction - clicked Enter after adding links
        app.blockAction(
            Pattern.compile("^inputBlock-process-\\w*"),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("In Main, Option selected in MenuView");
                        final String channelId = req.getPayload().getChannel().getId();
                        final String messageTs = req.getPayload().getContainer().getMessageTs();
                        final String userInput = req.getPayload().getActions().get(0).getValue();
                        ProcessView processView = ProcessView
                            .builder()
                            .channelId(channelId)
                            .messageTs(messageTs)
                            .userInput(userInput)
                            .build();
                        processView.start();
                    });
                return ctx.ack();
            }
        );

        // blockAction - clicked Query button or selected a session from the main menu
        app.blockAction(
            Pattern.compile("^buttonBlock-query-view-[-\\w]*"),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info("In Main, Starting up QueryView");
                        final String channelId = req.getPayload().getChannel().getId();
                        final String messageTs = req.getPayload().getContainer().getMessageTs();
                        final String actionId = req.getPayload().getActions().get(0).getActionId();
                        String requestId;
                        String queryText = "";

                        if (actionId.contains("click")) {
                            requestId = req.getPayload().getMessage().getText();
                        } else if (actionId.contains("select")) {
                            requestId =
                                req.getPayload().getActions().get(0).getSelectedOption().getValue();
                        } else if (actionId.contains("getcount-request")) {
                            requestId = req.getPayload().getMessage().getText();
                        } else if (actionId.contains("getnext-request")) {
                            requestId = req.getPayload().getMessage().getText();
                        } else if (actionId.contains("getcount-response")) {
                            requestId = req.getPayload().getMessage().getText();
                            queryText = req.getPayload().getActions().get(0).getValue();
                        } else if (actionId.contains("getnext-response")) {
                            requestId = req.getPayload().getMessage().getText();
                            queryText = req.getPayload().getActions().get(0).getValue();
                        } else {
                            requestId = "No active processed requests";
                        }

                        log.info("In Main, requestId: " + requestId);
                        log.info("In Main, queryText: " + queryText);
                        log.info("In Main, actionId: " + actionId);

                        if (requestId.equals("No active processed requests")) {
                            log.info("In Main, No active processed requests button clicked");
                            return;
                        }

                        QueryView queryView = QueryView
                            .builder()
                            .channelId(channelId)
                            .messageTs(messageTs)
                            .requestId(requestId)
                            .build();

                        QueryView.ViewType viewType;
                        if (actionId.contains("getcount-request")) {
                            viewType = QueryView.ViewType.GET_COUNT_REQUEST;
                        } else if (actionId.contains("getnext-request")) {
                            viewType = QueryView.ViewType.GET_NEXT_REQUEST;
                        } else if (actionId.contains("getcount-response")) {
                            viewType = QueryView.ViewType.GET_COUNT_RESPONSE;
                        } else if (actionId.contains("getnext-response")) {
                            viewType = QueryView.ViewType.GET_NEXT_RESPONSE;
                        } else {
                            viewType = QueryView.ViewType.NO_QUERY;
                        }

                        log.info("In Main, viewType: " + viewType);

                        queryView.start(viewType, queryText);
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
                        MenuView menuView = MenuView
                            .builder()
                            .channelId(channelId)
                            .messageTs(null)
                            .build();
                        menuView.start();
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
