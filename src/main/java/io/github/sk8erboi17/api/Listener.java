package io.github.sk8erboi17.api;

import io.github.sk8erboi17.utils.FailWriter;
import io.github.sk8erboi17.utils.ServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The Listener class is designed to handle incoming client connections to a server.
 * It utilizes asynchronous I/O provided by Java NIO to accept new connections and manage them using a callback mechanism.
 * The CompletionHandler for accepting connections is now a static inner class to avoid repeated object creation.
 */
public class Listener {
    private static final Logger log = LoggerFactory.getLogger(Listener.class);

    private static Listener instance;
    private AsynchronousServerSocketChannel serverSocketChannel;
    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * A SINGLE, STATIC, and REUSABLE CompletionHandler for accepting connections.
     * It is stateless and does not depend on an instance of the Listener class.
     */
    private static final CompletionHandler<AsynchronousSocketChannel, ConnectionRequest> acceptCompletionHandler = new CompletionHandler<>() {
        @Override
        public void completed(AsynchronousSocketChannel socketChannel, ConnectionRequest attachment) {
            try {
                if (attachment != null && socketChannel != null) {
                    attachment.onConnectionAccepted(socketChannel);
                }
            } catch (Exception e) {
                failed(e, attachment);
                return;
            }

            AsynchronousServerSocketChannel serverChannel = Listener.getInstance().getServerSocketChannel();
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.accept(attachment, this);
            }
        }

        @Override
        public void failed(Throwable exc, ConnectionRequest attachment) {
            log.error("Error with connection {}", exc.getMessage(), exc);
            FailWriter.writeFile("Error with connection ", exc);

            if (attachment != null) {
                attachment.onConnectionFailed(exc);
            }
        }
    };



    public static Listener getInstance() {
        if (instance == null) {
            instance = new Listener();
        }
        return instance;
    }


    public void startConnectionListen(AsynchronousServerSocketChannel serverSocketChannel, ConnectionRequest connectionRequest) {
        this.serverSocketChannel = serverSocketChannel;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.execute(() -> serverSocketChannel.accept(connectionRequest, acceptCompletionHandler));
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    public AsynchronousServerSocketChannel getServerSocketChannel() {
        return serverSocketChannel;
    }
}
