package io.github.sk8erboi17.api.input;

import io.github.sk8erboi17.api.Listener;
import io.github.sk8erboi17.exception.ServerAlreadyOpen;
import io.github.sk8erboi17.utils.FailWriter;
import io.github.sk8erboi17.utils.WelcomeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;

/**
 * *  The Connection Management from server side
 * This class provides a utility method for creating and binding an
 * AsynchronousServerSocketChannel to a specified network address (InetSocketAddress).
 * This channel listens for incoming connection requests from clients.
 * The method createInput opens a new server socket channel and binds it to the given address, preparing it to accept client connections.
 */
public class AsyncServerSocket {
    private static final Logger log = LoggerFactory.getLogger(AsyncServerSocket.class);
    private static AsynchronousServerSocketChannel serverSocketChannel;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_YELLOW = "\u001B[33m";

    public static AsynchronousServerSocketChannel createInput(InetSocketAddress inetSocketAddress) {
        if (serverSocketChannel != null) {
            throw new ServerAlreadyOpen("Server already open");
        }

        try {
            serverSocketChannel = AsynchronousServerSocketChannel.open().bind(inetSocketAddress);
            WelcomeMessage.logServerStatus(log, inetSocketAddress);
            shutdown();
            return serverSocketChannel;
        } catch (IOException e) {
            log.error("Error binding to port {}", inetSocketAddress.getPort(), e);
            throw new RuntimeException(e);
        }
    }


    private static void shutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            try {
                Listener.getInstance().closeListener();
                if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
                    serverSocketChannel.close();
                    log.info("Server socket closed");
                }

                try {
                    FailWriter.shutdown();
                } catch (Exception e) {
                    log.error("Error shutting down FailWriter: {}", e.getMessage());
                }
                log.info("Server shutdown complete");
            } catch (Exception e) {
                log.error("Error during shutdown: {}", e.getMessage());
                e.printStackTrace();
            }
        }));
    }
}
