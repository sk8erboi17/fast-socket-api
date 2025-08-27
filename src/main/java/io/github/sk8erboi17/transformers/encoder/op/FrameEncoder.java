package io.github.sk8erboi17.transformers.encoder.op;

import io.github.sk8erboi17.exception.MaxBufferSizeExceededException;
import io.github.sk8erboi17.listeners.callbacks.SendData;
import io.github.sk8erboi17.transformers.Markers;
import io.github.sk8erboi17.transformers.encoder.DataEncoder;
import io.github.sk8erboi17.transformers.pool.ByteBuffersPool;
import io.github.sk8erboi17.utils.FailWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Handles the logic for building and sending frames.
 * This class is a protocol-level component, focused on data serialization.
 * It uses a DataEncoder instance to perform the actual network I/O.
 */
public class FrameEncoder {

    private static final Logger log = LoggerFactory.getLogger(FrameEncoder.class);

    private final DataEncoder dataEncoder;
    private final ByteBuffersPool pool;

    public FrameEncoder(DataEncoder dataEncoder) {
        if (dataEncoder == null) {
            throw new IllegalArgumentException("DataEncoder cannot be null.");
        }
        this.dataEncoder = dataEncoder;
        this.pool = ByteBuffersPool.getInstance();
    }

    /**
     * Sends a heartbeat message to the server to keep the connection alive.
     * A heartbeat is a special frame with marker 0x00 and no payload.
     *
     * @param callback The callback to notify upon completion or failure.
     */
    public void sendHeartbeat(SendData callback) {
        byte marker = 0x00;
        int payloadSize = 0;
        Consumer<ByteBuffer> payloadWriter = buffer -> { /* No-op */ };
        buildAndSendFrame(marker, payloadSize, callback, payloadWriter);
    }

    public void sendInt(int data, SendData callback) {
        byte marker = 0x02;
        buildAndSendFrame(marker, Integer.BYTES, callback, buffer -> buffer.putInt(data));
    }

    public void sendString(String data, SendData callback) {
        byte marker = 0x01;
        byte[] stringBytes = data.getBytes(StandardCharsets.UTF_8);
        int payloadSize = stringBytes.length; // size of string data

        Consumer<ByteBuffer> payloadWriter = buffer -> {
            /*  How of string is send:
             *   L = LENGTH
             *   S = CONTENT
             *  -----------
             *  |L|SSSSS...|
             *  -----------
             */

            buffer.put(stringBytes);
        };
        buildAndSendFrame(marker, payloadSize, callback, payloadWriter);
    }

    public void sendFloat(float data, SendData callback) {
        byte marker = 0x03;
        buildAndSendFrame(marker, Float.BYTES, callback, buffer -> buffer.putFloat(data));
    }

    public void sendDouble(double data, SendData callback) {
        byte marker = 0x04;
        buildAndSendFrame(marker, Double.BYTES, callback, buffer -> buffer.putDouble(data));
    }

    public void sendChar(char data, SendData callback) {
        byte marker = 0x05;
        buildAndSendFrame(marker, Character.BYTES, callback, buffer -> buffer.putChar(data));
    }

    public void sendByteArray(byte[] data, SendData callback) {
        byte marker = 0x06;
        int payloadSize = data.length;
        Consumer<ByteBuffer> payloadWriter = buffer -> {
            buffer.put(data);
        };
        buildAndSendFrame(marker, payloadSize, callback, payloadWriter);
    }

    /**
     * The core method. It acquires a buffer, assembles the entire frame by
     * executing the 'payloadWriter', and initiates the asynchronous send.
     *
     * @param dataTypeMarker Marker for the data type.
     * @param payloadSize    The size in bytes of the payload only.
     * @param callback       The callback to notify of the result.
     * @param payloadWriter  The action that writes the payload into the provided buffer.
     *                       <p>
     *                       <b>EXAMPLE: Full Frame Structure</b>
     *                       <p>
     *                       This diagram shows the complete data frame for sending a string. The nested
     *                       diagram illustrates the structure of the payload itself.
     *<pre>
     * +--------------+-------------------+------------------+------------------------------------------------+
     * | START_MARKER |   FRAME_LENGTH      | dataTypeMarker  |                  Payload                       |
     * | (1 byte)     | (4 bytes=Int.BYTES)| (1 byte)         |                                                |
     * +--------------+-------------------+------------------+------------------------------------------------+
     * </pre>
     */
    private void buildAndSendFrame(byte dataTypeMarker, int payloadSize, SendData callback, Consumer<ByteBuffer> payloadWriter) {
        int totalPacketSize = Markers.START_MARKER_SIZE + Markers.DATA_TYPE_SIZE + payloadSize;

        ByteBuffer outputBuffer;
        try {
            outputBuffer = pool.acquire(totalPacketSize);
        } catch (InterruptedException | MaxBufferSizeExceededException e) {
            log.error("Failed to acquire buffer to send data. Cause: {}", e.getMessage());
            FailWriter.writeFile("Failed to acquire buffer to send data. Cause: ", e);
            if (callback != null) {
                callback.exception(e);
            }
            return;
        }

        if (outputBuffer.capacity() < totalPacketSize) {
            Exception e = new IllegalStateException("Acquired buffer is smaller than required packet size.");
            log.error("Buffer capacity error: required={}, actual={}", totalPacketSize, outputBuffer.capacity(), e);
            FailWriter.writeFile("Buffer capacity error. Cause: ", e);
            DataEncoder.releaseBuffer(pool, outputBuffer);
            if (callback != null) {
                callback.exception(e);
            }
        }

        try {
            outputBuffer.put(Markers.START_MARKER);
            outputBuffer.putInt(totalPacketSize);
            outputBuffer.put(dataTypeMarker);
            payloadWriter.accept(outputBuffer);
            outputBuffer.flip();
            dataEncoder.send(outputBuffer, callback);
        } catch (Exception e) {
            log.error("Error while assembling the frame", e);
            FailWriter.writeFile("Error while assembling the frame. Cause: ", e);
            DataEncoder.releaseBuffer(pool, outputBuffer);
            if (callback != null) {
                callback.exception(e);
            }
        }
    }
}
