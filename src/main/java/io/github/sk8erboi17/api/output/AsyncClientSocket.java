package io.github.sk8erboi17.api.output;

import io.github.sk8erboi17.listeners.group.PipelineGroupManager;
import io.github.sk8erboi17.utils.FailWriter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;

/**
 * The Connection Management from client side
 * This class is responsible for creating and managing AsynchronousSocketChannel instances on the client side.
 * The method createChannel initializes a new AsynchronousSocketChannel and connects it to a server address (InetSocketAddress).
 * It uses PipelineGroupManager to manage the underlying channel group, which can help in efficiently handling multiple concurrent connections.
 */
public class AsyncClientSocket {
    private static final PipelineGroupManager pipelineGroupManager = new PipelineGroupManager();

    public static AsynchronousSocketChannel createChannel(InetSocketAddress inetSocketAddress) {
        AsynchronousSocketChannel socketChannel;

        try {
            socketChannel = pipelineGroupManager.createChannel(inetSocketAddress);
        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new RuntimeException("Error while opening socket channel: " + e.getMessage(), e);
        }

        return socketChannel;
    }

    public static void closeChannelSocketChannel(AsynchronousSocketChannel socketChannel) {
        if (socketChannel != null && socketChannel.isOpen()) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to close socket channel", e);
            }
        }
    }


    /**
     * Shuts down the underlying static PipelineGroupManager, releasing all associated resources.
     * This method MUST be called once when the entire application using AsyncChannelSocket is shutting down,
     * not when individual channels are closed.
     */
    public static void shutdown() {
        pipelineGroupManager.shutdown();
        FailWriter.shutdown();
    }

}
