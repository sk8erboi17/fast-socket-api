package io.github.sk8erboi17.transformers.decoder;

import java.nio.ByteBuffer;

/**
 * Holds the context for a single read operation, including the temporary buffer.
 */
public class ReadOperationContext {
    private ReadContext parentContext;

    private ByteBuffer buffer;

    public ReadOperationContext(ReadContext parentContext, ByteBuffer buffer) {
        this.parentContext = parentContext;
        this.buffer = buffer;
    }

    public ReadContext getParentContext() {
        return parentContext;
    }

    public void setParentContext(ReadContext parentContext) {
        this.parentContext = parentContext;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
