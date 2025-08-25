package io.github.sk8erboi17.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class FailWriter {
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE_NAME = "error.log";
    private static final int BUFFER_SIZE = 65536;
    private static final int QUEUE_CAPACITY = 8192;
    private static final int MAX_STACKTRACE_LINES = 3;

    private static final AsynchronousFileChannel fileChannel;
    private static final BlockingQueue<String> messageQueue;
    private static final ExecutorService writerExecutor;
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final AtomicLong writePosition = new AtomicLong(0);
    private static final AtomicLong droppedMessages = new AtomicLong(0);

    private static final ThreadLocal<StringBuilder> stringBuilderPool =
            ThreadLocal.withInitial(() -> new StringBuilder(1024));

    static {
        try {
            Path logPath = Paths.get(LOG_DIR, LOG_FILE_NAME);
            Files.createDirectories(logPath.getParent());

            fileChannel = AsynchronousFileChannel.open(logPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);
            writePosition.set(Files.size(logPath));

            messageQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
            writerExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "AsyncFailWriter");
                t.setDaemon(true);
                return t;
            });

            startAsyncWriter();

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize AsyncFailWriter", e);
        }
    }

    private static void startAsyncWriter() {
        writerExecutor.submit(() -> {
            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            while (running.get() || !messageQueue.isEmpty()) {
                try {
                    String message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (message != null) {
                        byte[] data = message.getBytes(StandardCharsets.UTF_8);

                        if (data.length <= buffer.remaining()) {
                            buffer.put(data);
                        } else {
                            if (buffer.position() > 0) {
                                flushBuffer(buffer);
                            }

                            if (data.length <= buffer.capacity()) {
                                buffer.put(data);
                            } else {
                                writeDirectly(data);
                            }
                        }

                        int batchCount = 0;
                        while (batchCount < 20 && buffer.hasRemaining() &&
                                (message = messageQueue.poll()) != null) {
                            data = message.getBytes(StandardCharsets.UTF_8);
                            if (data.length <= buffer.remaining()) {
                                buffer.put(data);
                                batchCount++;
                            } else {
                                messageQueue.offer(message);
                                break;
                            }
                        }
                    } else if (buffer.position() > 0) {
                        flushBuffer(buffer);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("AsyncFailWriter error: " + e.getMessage());
                }
            }

            try {
                if (buffer.position() > 0) {
                    flushBuffer(buffer);
                }
            } catch (Exception e) {
                System.err.println("Final flush error: " + e.getMessage());
            }
        });
    }

    private static void flushBuffer(ByteBuffer buffer) {
        buffer.flip();
        long position = writePosition.getAndAdd(buffer.remaining());

        fileChannel.write(buffer, position, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attachment) {
                // Async completion - no action needed
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                System.err.println("Async write failed: " + exc.getMessage());
            }
        });

        buffer.clear();
    }

    private static void writeDirectly(byte[] data) {
        ByteBuffer directBuffer = ByteBuffer.wrap(data);
        long position = writePosition.getAndAdd(data.length);

        fileChannel.write(directBuffer, position, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attachment) {}

            @Override
            public void failed(Throwable exc, Void attachment) {
                System.err.println("Direct write failed: " + exc.getMessage());
            }
        });
    }

    public static void writeFile(String message, Throwable throwable) {
        if (!running.get()) return;

        try {
            StringBuilder sb = stringBuilderPool.get();
            sb.setLength(0);

            StackTraceElement[] stack = throwable.getStackTrace();
            String className = stack.length > 0 ? stack[0].getClassName() : "Unknown";
            String methodName = stack.length > 0 ? stack[0].getMethodName() : "Unknown";

            sb.append('[').append(Instant.now()).append("] ERROR in ")
                    .append(className).append('.').append(methodName).append("(): ")
                    .append(message).append(" - ").append(throwable.getMessage())
                    .append(System.lineSeparator());

            int stackLines = Math.min(stack.length, MAX_STACKTRACE_LINES);
            for (int i = 0; i < stackLines; i++) {
                sb.append("  at ").append(stack[i]).append(System.lineSeparator());
            }

            if (stack.length > MAX_STACKTRACE_LINES) {
                sb.append("  ... ").append(stack.length - MAX_STACKTRACE_LINES)
                        .append(" more").append(System.lineSeparator());
            }

            boolean queued = messageQueue.offer(sb.toString(), 1, TimeUnit.MILLISECONDS);
            if (!queued) {
                droppedMessages.incrementAndGet();
            }

        } catch (Exception e) {
            System.err.println("AsyncFailWriter failed: " + e.getMessage());
        }
    }

    public static void writeFileLight(String message, String error) {
        if (!running.get()) return;

        StringBuilder sb = stringBuilderPool.get();
        sb.setLength(0);
        sb.append('[').append(System.currentTimeMillis()).append("] ")
                .append(message).append(": ").append(error)
                .append(System.lineSeparator());

        messageQueue.offer(sb.toString());
    }

    public static void shutdown() {
        running.set(false);
        try {
            writerExecutor.shutdown();
            if (!writerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                writerExecutor.shutdownNow();
            }
            fileChannel.close();

            long dropped = droppedMessages.get();
            if (dropped > 0) {
                System.out.println("AsyncFailWriter: " + dropped + " messages dropped");
            }
        } catch (Exception e) {
            System.err.println("Shutdown error: " + e.getMessage());
        }
    }

    public static long getDroppedCount() {
        return droppedMessages.get();
    }

    public static int getQueueSize() {
        return messageQueue.size();
    }
}