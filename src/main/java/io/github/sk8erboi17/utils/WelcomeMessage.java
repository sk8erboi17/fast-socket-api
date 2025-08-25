package io.github.sk8erboi17.utils;

import org.slf4j.Logger;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class to generate and log a formatted, colored server startup banner.
 * This class handles dynamic alignment and fetches configuration from ServerOptions.
 */
public final class WelcomeMessage {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_GREEN = "\u001B[32m";

    private WelcomeMessage() {}

    /**
     * Builds and logs the server startup banner by fetching configuration
     * directly from the ServerOptions singleton.
     *
     * @param log     The SLF4J logger instance to use.
     * @param address The InetSocketAddress the server is bound to.
     */
    public static void logServerStatus(Logger log, InetSocketAddress address) {

        ServerOptions options = ServerOptions.getInstance();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("Server Name", options.getServerName());
        data.put("Listening on Port", address.getPort());
        data.put("Worker Threads", options.getThreadsNumber());
        data.put("Buffer Pools", options.getBufferPools());
        data.put("Keep-Alive", String.format("%s (Timeout: %ds)", options.isKeepAlive(), options.getTimeout()));
        data.put("Java Version", System.getProperty("java.version"));
        data.put("Status", "ONLINE");

        int maxKeyLength = data.keySet().stream()
                .mapToInt(String::length)
                .max()
                .orElse(0);

        StringBuilder sb = new StringBuilder("\n");
        sb.append(ANSI_CYAN).append("┌─ S E R V E R   S T A T U S ").append("─".repeat(45)).append("┐").append(ANSI_RESET).append("\n");
        sb.append(ANSI_CYAN).append("│ ").append("─".repeat(74)).append(ANSI_RESET).append("\n");

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String formattedKey = String.format("%-" + maxKeyLength + "s", key);

            sb.append(ANSI_CYAN).append("│ ").append(formattedKey).append(" : ").append(ANSI_RESET);

            if (key.equals("Server Name")) {
                sb.append(ANSI_YELLOW).append(value).append(ANSI_RESET);
            } else if (key.equals("Status")) {
                sb.append(ANSI_GREEN).append(value).append(ANSI_RESET);
            } else {
                sb.append(ANSI_WHITE).append(value).append(ANSI_RESET);
            }

            int valueLength = value.toString().length();
            int lineLength = 2 + maxKeyLength + 3 + valueLength;
            int padding = 74 - lineLength;
            sb.append(" ".repeat(Math.max(0, padding)));

            sb.append(ANSI_CYAN).append(" │").append(ANSI_RESET).append("\n");
        }

        sb.append(ANSI_CYAN).append("└").append("─".repeat(74)).append("┘").append(ANSI_RESET).append("\n");

        log.info(sb.toString());
    }
}