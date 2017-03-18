package edu.vanderbilt.yunyul.vertxtw;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;
import lombok.Setter;

import java.util.regex.Pattern;

import static edu.vanderbilt.yunyul.vertxtw.TwitterWallBootstrap.log;

public class TweetBroadcaster {
    @Setter
    private TwitterHandler twitterHandler;

    private final Context context;
    private final Table<String, SockJSSocket, Boolean> channels = HashBasedTable.create();
    private static final Pattern hashtag = Pattern.compile("^\\w+$");

    public TweetBroadcaster(Router router, Vertx vertx) {
        log("Initializing broadcaster...");

        this.context = vertx.getOrCreateContext();
        SockJSHandlerOptions options = new SockJSHandlerOptions().setHeartbeatInterval(2000);
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);

        sockJSHandler.socketHandler(sock -> {
            sock.handler(buffer -> {
                String msg = buffer.toString();

                String[] parts = msg.split(" ");

                String cmd = parts[0];
                String channel = parts[1].toLowerCase();

                switch (cmd.toUpperCase()) {
                    // Registration command
                    case "REG":
                        if (channel.length() > 0 && channel.length() <= 30 && hashtag.matcher(channel).matches()) {
                            channels.put(channel, sock, true);
                            twitterHandler.trackTag(channel);
                        }
                        break;
                    // Unregister command
                    case "UNREG":
                        removeClientFromTag(channel, sock);
                }
            });
            sock.endHandler(v -> {
                for (String channel : channels.column(sock).keySet()) {
                    removeClientFromTag(channel, sock);
                }
            });
        });

        router.route("/sock/*").handler(sockJSHandler);
    }

    private void removeClientFromTag(String channel, SockJSSocket sock) {
        channels.remove(channel, sock);
        if (!channels.containsRow(channel)) {
            twitterHandler.untrackTag(channel);
        }
    }

    /**
     * Broadcasts the specified message to all channels associated with the specified tag
     *
     * @param tag  Tag to broadcast the message to
     * @param text The message to send
     */
    public void broadcast(String tag, String text) {
        context.runOnContext(v -> channels.row(tag.toLowerCase()).keySet()
                        .forEach(client -> client.write(Buffer.buffer(text))));
    }
}
