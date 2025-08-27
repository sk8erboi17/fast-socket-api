package io.github.sk8erboi17.api.pipeline.in;

import io.github.sk8erboi17.listeners.callbacks.ReceiveData;
import io.github.sk8erboi17.transformers.decoder.DataDecoder;
import io.github.sk8erboi17.transformers.decoder.op.FrameDecoder;
import io.github.sk8erboi17.transformers.encoder.op.ListenData;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * The PipelineIn class correctly initializes AND MANAGES the entire inbound
 * data pipeline for a SINGLE client connection.
 */
public class PipelineIn {

    // 1. Aggiungi un campo per mantenere un riferimento al DataDecoder
    private final DataDecoder dataDecoder;

    /**
     * Constructor to initialize and start the inbound data pipeline for a new client.
     *
     * @param client          The newly accepted AsynchronousSocketChannel for the client.
     * @param maxFrameLength  The maximum allowed size for a single data frame.
     * @param initialCallback The initial callback to handle the first piece of data.
     */
    public PipelineIn(AsynchronousSocketChannel client, int maxFrameLength, ReceiveData initialCallback) {
        ListenData listenDataProcessor = new ListenData();
        FrameDecoder frameDecoder = new FrameDecoder(maxFrameLength, listenDataProcessor);

        this.dataDecoder = new DataDecoder(client, initialCallback, frameDecoder);

        this.dataDecoder.startDecoding();
    }

    /**
     * change handler for incoming datas
     *
     * @param newCallback Il nuovo ReceiveData handler.
     */
    public void setReceiveDataCallback(ReceiveData newCallback) {
        this.dataDecoder.setReceiveDataCallback(newCallback);
    }
}