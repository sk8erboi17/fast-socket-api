package io.github.sk8erboi17.api;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * An interface for handling server connection events.
 */
public interface ConnectionRequest {
    /**
     * Handles a newly accepted client connection.
     * The application's send/receive logic should be implemented here,
     * for example, by using the Conversation class.
     *
     * @param socketChannel The accepted AsynchronousSocketChannel for the client.
     */
    void onConnectionAccepted(AsynchronousSocketChannel socketChannel);

    /**
     * Handles a failure during the connection acceptance process.
     * This is typically called when the server fails to accept a new connection.
     *
     * @param exc The exception that occurred.
     */
    void onConnectionFailed(Throwable exc);


}