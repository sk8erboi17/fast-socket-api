package io.github.sk8erboi17.transformers.decoder;

import io.github.sk8erboi17.listeners.callbacks.ReceiveData;
import io.github.sk8erboi17.transformers.decoder.op.FrameDecoder;
import io.github.sk8erboi17.transformers.pool.ByteBuffersPool;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * Holds the long-lived, stateful components for the entire connection.
 */
public class ReadContext {

    private AsynchronousSocketChannel channel;
    private FrameDecoder frameDecoder;
    private ReceiveData callback;
    private ByteBuffersPool pool;

    public ReadContext(AsynchronousSocketChannel channel, FrameDecoder frameDecoder, ReceiveData callback, ByteBuffersPool pool) {
        this.channel = channel;
        this.frameDecoder = frameDecoder;
        this.pool = pool;
        this.callback = callback;
    }

    public FrameDecoder getFrameDecoder() {
        return frameDecoder;
    }

    public void setFrameDecoder(FrameDecoder frameDecoder) {
        this.frameDecoder = frameDecoder;
    }

    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public void setChannel(AsynchronousSocketChannel channel) {
        this.channel = channel;
    }

    public ByteBuffersPool getPool() {
        return pool;
    }

    public void setPool(ByteBuffersPool pool) {
        this.pool = pool;
    }

    public ReceiveData getCallback() {
        return callback;
    }

    public void setCallback(ReceiveData callback) {
        this.callback = callback;
    }
}


