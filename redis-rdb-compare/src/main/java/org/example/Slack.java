package org.example;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;

public class Slack {
public static void main(String[] args) {
        // APP expects SLACK_APP_TOKEN, SLACK_BOT_TOKEN
        System.out.println("Hello world, starting bot!");
        var app = new App();
        app.command("/ping", (req, ctx) -> ctx.ack(":wave: Pong!"));
        app.command("/parse", (req, ctx) -> {
            System.out.println("/parse called...");
            return ctx.ack(SlackHelper.parseViaSlack());
        });
        try {
            new SocketModeApp(app).start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
