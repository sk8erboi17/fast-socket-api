package io.github.sk8erboi17.exception;

public class ServerAlreadyOpen extends RuntimeException {
    public ServerAlreadyOpen(String message) {
        super(message);
    }

    public ServerAlreadyOpen(String message, Throwable cause) {
        super(message, cause);
    }
}