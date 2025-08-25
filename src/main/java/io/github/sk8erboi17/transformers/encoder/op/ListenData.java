package io.github.sk8erboi17.transformers.encoder.op;

import io.github.sk8erboi17.exception.ProtocolIncompleteException;
import io.github.sk8erboi17.exception.ProtocolViolationException;
import io.github.sk8erboi17.listeners.callbacks.ReceiveData;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ListenData {

    private static final int INT_BYTES = Integer.BYTES;
    private static final int FLOAT_BYTES = Float.BYTES;
    private static final int DOUBLE_BYTES = Double.BYTES;
    private static final int CHAR_BYTES = Character.BYTES;

    public void listen(byte marker, ByteBuffer buffer, ReceiveData callback) {
        if (marker == 0x00) {
            handleHeartbeat(callback);
            return;
        }
        if (buffer.remaining() == 0) {
            callback.exception(new IllegalArgumentException("The provided buffer is empty. Cannot process data."));
            return;
        }

        try {
            switch (marker) {
                case 0x01 -> handleString(buffer, callback);
                case 0x02 -> handleInt(buffer, callback);
                case 0x03 -> handleFloat(buffer, callback);
                case 0x04 -> handleDouble(buffer, callback);
                case 0x05 -> handleChar(buffer, callback);
                case 0x06 -> handleByteArray(buffer, callback);
                default ->
                        callback.exception(new ProtocolViolationException("Unknown marker received: 0x" + String.format("%02X", marker) + ". Remaining buffer: " + buffer.remaining() + " bytes."));
            }
        } catch (BufferUnderflowException e) {
            callback.exception(new ProtocolIncompleteException("Insufficient data in the buffer for the data type expected by marker 0x" + String.format("%02X", marker) + ". Incomplete or malformed message.", e));
        } catch (Exception e) {
            callback.exception(new RuntimeException("Unexpected error while processing data with marker 0x" + String.format("%02X", marker) + ": " + e.getMessage(), e));
        }
    }

    private void handleHeartbeat(ReceiveData callback) {
        // No data needs to be processed for a heartbeat.
        callback.receive(null);
    }

    private void handleString(ByteBuffer buffer, ReceiveData callback) {
        if (buffer.remaining() < INT_BYTES) {
            throw new BufferUnderflowException();
        }
        int length = buffer.getInt();

        if (length < 0) {
            throw new ProtocolViolationException("Protocol violation: Invalid negative string length received: " + length);
        }
        if (length > buffer.remaining()) {
            throw new ProtocolViolationException("Protocol violation: Stated string length " + length + " is greater than remaining buffer size " + buffer.remaining());
        }


        byte[] stringBytes = new byte[length];
        buffer.get(stringBytes);
        String data = new String(stringBytes, StandardCharsets.UTF_8);
        callback.receive(data);
    }

    private void handleInt(ByteBuffer buffer, ReceiveData callback) {
        if (buffer.remaining() < INT_BYTES) {
            throw new BufferUnderflowException();
        }
        int data = buffer.getInt();
        callback.receive(data);
    }

    private void handleFloat(ByteBuffer buffer, ReceiveData callback) {
        if (buffer.remaining() < FLOAT_BYTES) {
            throw new BufferUnderflowException();
        }
        float data = buffer.getFloat();
        callback.receive(data);
    }

    private void handleDouble(ByteBuffer buffer, ReceiveData callback) {
        if (buffer.remaining() < DOUBLE_BYTES) {
            throw new BufferUnderflowException();
        }
        double data = buffer.getDouble();
        callback.receive(data);
    }

    private void handleChar(ByteBuffer buffer, ReceiveData callback) {
        if (buffer.remaining() < CHAR_BYTES) {
            throw new BufferUnderflowException();
        }
        char data = buffer.getChar();
        callback.receive(data);
    }

    private void handleByteArray(ByteBuffer buffer, ReceiveData callback) {
        if (buffer.remaining() < INT_BYTES) {
            throw new BufferUnderflowException();
        }
        int length = buffer.getInt();

        if (length < 0) {
            throw new ProtocolViolationException("Protocol violation: Invalid negative byte array length received: " + length);
        }
        if (length > buffer.remaining()) {
            throw new ProtocolViolationException("Protocol violation: Stated byte array length " + length + " is greater than remaining buffer size " + buffer.remaining());
        }

        byte[] data = new byte[length];
        buffer.get(data);
        callback.receive(data);
    }
}