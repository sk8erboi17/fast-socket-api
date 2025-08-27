package io.github.sk8erboi17.transformers.pool;

import io.github.sk8erboi17.exception.MaxBufferSizeExceededException;
import io.github.sk8erboi17.utils.FailWriter;
import io.github.sk8erboi17.utils.ServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ByteBuffersPool {

    public static final int LARGE_SIZE = 65536;
    private static final Logger log = LoggerFactory.getLogger(ByteBuffersPool.class);
    private static final int SMALL_SIZE = 256;
    private static final int MEDIUM_SIZE = 4096;
    private static volatile ByteBuffersPool instance; /* lazy because need to instance before ServerOptions.java */
    private final BlockingQueue<ByteBuffer> smallBuffers;
    private final BlockingQueue<ByteBuffer> mediumBuffers;
    private final BlockingQueue<ByteBuffer> largeBuffers;

    private ByteBuffersPool(int poolSize) {
        this.smallBuffers = new ArrayBlockingQueue<>(poolSize);
        this.mediumBuffers = new ArrayBlockingQueue<>(poolSize);
        this.largeBuffers = new ArrayBlockingQueue<>(poolSize);

        log.info("Initializing ByteBuffer pool with a size of {} for each buffer type.", poolSize);

        for (int i = 0; i < poolSize; i++) {
            try {
                smallBuffers.put(ByteBuffer.allocateDirect(SMALL_SIZE));
                mediumBuffers.put(ByteBuffer.allocateDirect(MEDIUM_SIZE));
                largeBuffers.put(ByteBuffer.allocateDirect(LARGE_SIZE));
            } catch (InterruptedException e) {
                log.error("Interrupted during pool creation.", e);
                FailWriter.writeFile("Error while creating pool", e);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Returns the singleton instance of the ByteBuffersPool.
     * <p>
     * This method uses lazy initialization, meaning the pool instance is created only when it is first requested.
     *
     * <b>Why is it synchronized?</b>
     * To prevent a <b>race condition</b> in a multithreaded environment.
     * Without synchronization, if two or more threads call {@code getInstance()} at the exact same time when {@code instance}
     * is {@code null}, both threads could pass the {@code if (instance == null)} check simultaneously. This would result
     * in multiple instances of the pool being created, which violates the Singleton pattern and could lead to resource leaks or unpredictable behavior.
     *
     * <b>How does it work?</b>
     * The {@code synchronized} keyword ensures <b>mutual exclusion</b> by forcing threads to acquire a lock before they can
     * execute the method. For a static method, the lock is on the class object itself ({@code ByteBuffersPool.class}). The sequence of events is as follows:
     * <ol>
     * <li><b>Thread A</b> is the first to call {@code getInstance()}. It acquires the lock and enters the method.</li>
     * <li>It checks {@code if (instance == null)}, which is true, and begins to create the new {@code ByteBuffersPool} instance.</li>
     * <li>While Thread A is creating the instance, <b>Thread B</b> calls {@code getInstance()}. It attempts to acquire the lock, but since Thread A holds it, Thread B is <b>blocked</b> and forced to wait.</li>
     * <li>Thread A finishes creating the object, assigns it to the {@code instance} variable, and then exits the method, releasing the lock.</li>
     * <li>Now, Thread B can acquire the lock and enter the method. It checks {@code if (instance == null)}, but the condition is now false because Thread A already created the instance.</li>
     * <li>Thread B skips the creation block and simply returns the already-existing {@code instance}.</li>
     * </ol>
     * This mechanism guarantees that the critical section of code that creates the instance is executed exactly once, safely establishing the singleton.
     *
     * @return The single, shared instance of the ByteBuffersPool.
     * <p>
     * <p>
     * We use <a href="https://en.wikipedia.org/wiki/Double-checked_locking">Double-Checked Locking Pattern</a>
     */

    public static ByteBuffersPool getInstance() {
        if (instance == null) {
            synchronized (ByteBuffersPool.class) {
                if (instance == null) {
                    int configuredPoolSize = ServerOptions.getInstance().getBufferPools();
                    instance = new ByteBuffersPool(configuredPoolSize);
                }
            }
        }
        return instance;
    }

    public ByteBuffer acquire(int size) throws InterruptedException, MaxBufferSizeExceededException {
        ByteBuffer byteBuffer;
        if (size <= SMALL_SIZE) {
            byteBuffer = smallBuffers.take();
        } else if (size <= MEDIUM_SIZE) {
            byteBuffer = mediumBuffers.take();
        } else if (size <= LARGE_SIZE) {
            byteBuffer = largeBuffers.take();
        } else {
            throw new MaxBufferSizeExceededException("Requested size (" + size + ") exceeds maximum buffer size (" + LARGE_SIZE + ")");
        }

        byteBuffer.clear();
        return byteBuffer;
    }

    public void release(ByteBuffer byteBuffer) throws InterruptedException {
        if (byteBuffer == null) {
            throw new NullPointerException("Null buffer size");
        }
        int size = byteBuffer.capacity();

        if (size != SMALL_SIZE && size != MEDIUM_SIZE && size != LARGE_SIZE) {
            throw new IllegalArgumentException("Attempted to release a buffer with an illegal capacity: " + size);
        }

        if (size == SMALL_SIZE) {
            smallBuffers.put(byteBuffer);
        } else if (size == MEDIUM_SIZE) {
            mediumBuffers.put(byteBuffer);
        } else {
            largeBuffers.put(byteBuffer);
        }

    }


}