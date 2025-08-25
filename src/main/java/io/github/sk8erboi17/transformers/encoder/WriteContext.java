package io.github.sk8erboi17.transformers.encoder;

import io.github.sk8erboi17.listeners.callbacks.SendData;
import io.github.sk8erboi17.transformers.pool.ByteBuffersPool;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * A record to hold the state of a single write operation.
 * It contains the buffer, the original callback, and references to the
 * necessary components for the completion handler to be fully stateless.
 */
public record WriteContext(ByteBuffer buffer, SendData sendData, AsynchronousSocketChannel channel, ByteBuffersPool pool) {
}
