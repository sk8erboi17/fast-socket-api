package io.github.sk8erboi17.transformers.decoder.op;

import io.github.sk8erboi17.listeners.callbacks.ReceiveData;
import io.github.sk8erboi17.transformers.Markers;
import io.github.sk8erboi17.transformers.encoder.op.ListenData;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optimized and Secure SocketFrameDecoder for high performance.
 * Responsible for framing messages based on a length-prefixed protocol.
 * The protocol is [START_MARKER (1 byte)][LENGTH (4 bytes)][DATA_TYPE (1 byte)][PAYLOAD (N bytes)]
 */
public class FrameDecoder {

    private static final Logger LOGGER = Logger.getLogger(FrameDecoder.class.getName());

    private static final byte START_MARKER = 0x01;

    final int MAX_GARBAGE_TOLERANCE = 8192;

    private final int maxFrameLength;
    private final ListenData listenDataProcessor;

    public FrameDecoder(int maxFrameLength, ListenData listenDataProcessor) {
        if (maxFrameLength <= 0) {
            throw new IllegalArgumentException("Buffer sizes must be positive.");
        }
        if (listenDataProcessor == null) {
            throw new IllegalArgumentException("listenDataProcessor cannot be null.");
        }

        this.maxFrameLength = maxFrameLength;
        this.listenDataProcessor = listenDataProcessor;
    }

    public void decode(Channel clientChannel, ByteBuffer buffer, ReceiveData callback) {
        processFrames(clientChannel, buffer, callback);
    }

    private void processFrames(Channel channel, ByteBuffer buffer, ReceiveData callback) {
        while (true) {
            if (!findAndSkipToStartMarker(buffer, MAX_GARBAGE_TOLERANCE)) {
                return;
            }

            buffer.mark();

            if (buffer.remaining() < Integer.BYTES) {
                buffer.reset();
                return;
            }

            int frameLength = buffer.getInt(); // read frame length


            /* check if buffer overflow*/
            if (frameLength <= 0 || frameLength > maxFrameLength) {
                logError("Invalid frame length received: " + frameLength + ". Max allowed: " + maxFrameLength + ". Closing connection.", new BufferOverflowException(), channel);
                try {
                    channel.close();
                } catch (IOException e) {
                    logError("Error closing channel after invalid frame length: " + e.getMessage(), channel);
                }
                buffer.clear();
                return;
            }

            int actualPayloadSize = frameLength - Markers.START_MARKER_SIZE - Markers.DATA_TYPE_SIZE;

            // Check if the full frame is available in the buffer.
            if (buffer.remaining() < actualPayloadSize) {
                buffer.reset();
                return;
            }

            try {
                byte dataTypeMarker = buffer.get();
                ByteBuffer dataTypeBuffer = buffer.slice();
                listenDataProcessor.listen(dataTypeMarker, dataTypeBuffer, actualPayloadSize, callback); // buffer contain only payload
                buffer.position(buffer.position() + actualPayloadSize);
            } catch (Exception e) {
                logError("Error processing decoded frame", e, channel);
            }
        }
    }

    private boolean findAndSkipToStartMarker(ByteBuffer buffer, int scanLimit) {
        int bytesScanned = 0;
        while (buffer.hasRemaining() && bytesScanned < scanLimit) {
            if (buffer.get() == START_MARKER) {
                return true;
            }
            bytesScanned++;
        }
        return false;
    }

    private void logError(String message, Channel channel) {
        if (LOGGER.isLoggable(Level.WARNING)) {
            String channelInfo = "Channel: " + channel.toString();
            try {
                if (channel instanceof AsynchronousSocketChannel) {
                    channelInfo = "Client: " + ((AsynchronousSocketChannel) channel).getRemoteAddress().toString();
                } else if (channel instanceof SocketChannel) {
                    channelInfo = "Client: " + ((SocketChannel) channel).getRemoteAddress().toString();
                }
            } catch (IOException e) {
                channelInfo += " (failed to get remote address)";
            }
            LOGGER.warning("WARNING: " + message + " " + channelInfo);
        }
    }

    private void logError(String message, Exception e, Channel channel) {
        if (LOGGER.isLoggable(Level.WARNING)) {
            String channelInfo = "Channel: " + channel.toString();
            try {
                if (channel instanceof AsynchronousSocketChannel) {
                    channelInfo = "Client: " + ((AsynchronousSocketChannel) channel).getRemoteAddress().toString();
                } else if (channel instanceof SocketChannel) {
                    channelInfo = "Client: " + ((SocketChannel) channel).getRemoteAddress().toString();
                }
            } catch (IOException ioException) {
                channelInfo += " (failed to get remote address)";
            }
            String fullMessage = String.format("ERROR: %s %s", message, channelInfo);
            LOGGER.log(Level.WARNING, fullMessage, e);
        }
    }

}
