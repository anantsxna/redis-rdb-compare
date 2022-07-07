package org.example;

import static org.example.SlackUtils.*;
import static org.messaging.PostUpdate.postTextResponseAsync;
import static org.messaging.PostUpdateUtils.deleteResponseAsync;
import static org.messaging.PostUpdateUtils.postResponseAsync;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageChangedEvent;
import com.slack.api.model.event.MessageDeletedEvent;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.util.thread.DaemonThreadExecutorServiceProvider;
import java.util.List;
import java.util.Properties;
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

    public static Properties props = new Properties();

    static {
        try {
            props.load(
                Thread
                    .currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("application.properties")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method for the application.
     */
    public static void main(String[] args) {
        var app = new App(
            AppConfig
                .builder()
                .executorServiceProvider(DaemonThreadExecutorServiceProvider.getInstance())
                .signingSecret(System.getenv("SLACK_SIGNING_SECRET"))
                .threadPoolSize(Integer.parseInt(props.getProperty("THREAD_POOL_SIZE")))
                .build()
        );

        // RE: command "/ping" - responds with "pong"
        app.command(
            "/ping",
            (req, ctx) -> {
                log.info(props.getProperty("PING_RECEIVED"));
                return ctx.ack(props.getProperty("PING_RESPONSE"));
            }
        );

        // RE: command "/session" - opens a new bot session
        app.command(
            "/session",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info(props.getProperty("SESSION_RECEIVED"));
                        final String channelId = req.getContext().getChannelId();
                        final String response = createSessionUtils();
                        log.info(props.getProperty("SESSION_RESPONSE"), response);
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
                        log.info(props.getProperty("PROCESS_RECEIVED"));
                        final String channelId = req.getContext().getChannelId();
                        final String text = req.getPayload().getText();
                        String response = processAllUtils(text, channelId);
                        log.info(props.getProperty("PROCESS_RESPONSE"), response);
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
                        log.info(props.getProperty("DOWNLOAD_RECEIVED"), text);
                        final String response = downloadUtils(text, channelId, false);
                        log.info(props.getProperty("DOWNLOAD_RESPONSE"), response, channelId);
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
                        log.info(props.getProperty("PARSE_RECEIVED"));
                        String response = parseUtils(requestId, channelId, false);
                        log.info(props.getProperty("PARSE_RESPONSE"), response, channelId);
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
                        log.info(props.getProperty("MAKETRIE_RECEIVED"));
                        final String channelId = req.getContext().getChannelId();
                        final String requestId = req.getPayload().getText();
                        String response = makeTrieUtils(requestId, channelId, false);
                        log.info(props.getProperty("MAKETRIE_RESPONSE"), response, channelId);
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
                        log.info(props.getProperty("GETCOUNT_RECEIVED"));
                        final String channelId = req.getContext().getChannelId();
                        final String text = req.getPayload().getText();
                        List<LayoutBlock> responseBlocks = countUtils(text);
                        log.info(props.getProperty("GETCOUNT_RESPONSE"), responseBlocks);
                        postResponseAsync(responseBlocks, channelId, "OK 200");
                    });
                return ctx.ack();
            }
        );

        // RE: command "/getnext [requestId] [prefixKey] [n]" - gets 'n' keys with the prefixKey in all tries
        //        app.command(
        //            "/getnext",
        //            (req, ctx) -> {
        //                app
        //                    .executorService()
        //                    .submit(() -> {
        //                        log.info(props.getProperty("GETNEXT_RECEIVED"));
        //                        final String channelId = req.getContext().getChannelId();
        //                        final String text = req.getPayload().getText();
        //                        String response = getNextKeyUtils(text);
        //                        log.info(props.getProperty("GETNEXT_RESPONSE"), response);
        //                        postTextResponseAsync(response, channelId);
        //                    });
        //                return ctx.ack();
        //            }
        //        );

        // RE: command "/clear [requestId]" - resets the sessions with the given requestId
        app.command(
            "/clear",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info(props.getProperty("CLEAR_RECEIVED"));
                        final String channelId = req.getContext().getChannelId();
                        String requestId = req.getPayload().getText();
                        String response = deleteSessionUtils(requestId);
                        log.info(props.getProperty("CLEAR_RESPONSE"), response);
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
                        log.info(props.getProperty("CLEARALL_RECEIVED"));
                        final String channelId = req.getContext().getChannelId();
                        String response = deleteAllSessionsUtils();
                        log.info(props.getProperty("CLEARALL_RESPONSE"), response);
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
                        log.info(props.getProperty("LIST_RECEIVED"));
                        final String channelId = req.getContext().getChannelId();
                        String response = listSessionsUtils();
                        log.info(props.getProperty("LIST_RESPONSE"), response);
                        postTextResponseAsync(response, channelId);
                    });
                return ctx.ack();
            }
        );

        // RE: command "/redis-bot-help" - provides help for the bot
        app.command(
            "/redis-bot-help",
            (req, ctx) -> {
                log.info(props.getProperty("HELP_RECEIVED"));
                return ctx.ack(props.getProperty("HELP_RESPONSE"));
            }
        );

        // command "/menu" - starts interactive session
        app.command(
            "/menu",
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info(props.getProperty("MENU_RECEIVED"));
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
            Pattern.compile(props.getProperty("CLOSE_BUTTON_PATTERN")),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info(props.getProperty("CLOSE_BUTTON_CLICKED"));
                        final String channelId = req.getPayload().getChannel().getId();
                        final String messageTs = req.getPayload().getContainer().getMessageTs();
                        deleteResponseAsync(channelId, messageTs);
                    });
                return ctx.ack();
            }
        );

        // blockAction - clicked 'Delete Session' button
        app.blockAction(
            Pattern.compile(props.getProperty("DELETE_SESSION_BUTTON_PATTERN")),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info(props.getProperty("DELETE_SESSION_BUTTON_CLICKED"));
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
            Pattern.compile(props.getProperty("NEW_SESSION_BUTTON_PATTERN")),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info(props.getProperty("NEW_SESSION_BUTTON_CLICKED"));
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
            Pattern.compile(props.getProperty("LINKS_INPUT")),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info(props.getProperty("LINKS_INPUT_ENTERED"));
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
            Pattern.compile(props.getProperty("QUERY_GENERAL_INPUT")),
            (req, ctx) -> {
                app
                    .executorService()
                    .submit(() -> {
                        log.info(props.getProperty("QUERY_INPUT_ENTERED"));
                        final String channelId = req.getPayload().getChannel().getId();
                        final String messageTs = req.getPayload().getContainer().getMessageTs();
                        final String actionId = req.getPayload().getActions().get(0).getActionId();
                        String requestId = "";
                        String queryText = "!root";

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
                            if (actionId.contains("search")) {
                                //                                        log.info(req.getRequestBodyAsString());
                                String requestId_queryText = actionId
                                    .replaceFirst("buttonBlock-query-search-getcount-response-", "")
                                    .replaceFirst("-[-\\w]*", "");
                                log.info("Here: {}", requestId_queryText);
                                requestId = requestId_queryText.split("%")[0];
                                String parentKey = requestId_queryText.split("%")[1];
                                String head = requestId_queryText.split("%")[2];
                                queryText = parentKey + " " + head;
                            } else {
                                requestId = req.getPayload().getMessage().getText();
                                queryText = req.getPayload().getActions().get(0).getValue();
                            }
                        } else if (actionId.contains("getnext-response")) {
                            requestId = req.getPayload().getMessage().getText();
                            queryText = req.getPayload().getActions().get(0).getValue();
                        } else {
                            requestId = props.getProperty("NO_ACTIVE_REQUESTS");
                        }

                        if (requestId.equals(props.getProperty("NO_ACTIVE_REQUESTS"))) {
                            log.info(props.getProperty("NO_ACTIVE_REQUESTS"));
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

                        log.info(props.getProperty("RENDER_QUERY_VIEW") + viewType);

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
                        log.info(props.getProperty("APP_MENTION_RECEIVED"));
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
                    log.info(props.getProperty("SHUTDOWN_HOOK_STARTED"));
                    deleteAllSessionsUtils();
                    app.executorService().shutdown();
                    try {
                        boolean exit0 = app
                            .executorService()
                            .awaitTermination(10, TimeUnit.SECONDS);
                        assert !exit0 : props.getProperty("SHUTDOWN_HOOK_FAILED");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
            );

        //start the app
        try {
            SocketModeApp socketModeApp = new SocketModeApp(app);
            log.info(props.getProperty("APP_STARTED"));
            socketModeApp.start();
        } catch (OutOfMemoryError e) {
            log.error(props.getProperty("OUT_OF_MEMORY"), e.getMessage());
            deleteAllSessionsUtils();
            app.executorService().shutdownNow();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
