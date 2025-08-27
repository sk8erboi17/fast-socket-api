package example;

import example.requests.SendEcho;
import example.responses.ReceiverOne;
import example.responses.ReceiverTwo;
import io.github.sk8erboi17.api.ConnectionRequest;
import io.github.sk8erboi17.api.Listener;
import io.github.sk8erboi17.api.input.AsyncServerSocket;
import io.github.sk8erboi17.api.pipeline.in.PipelineIn;
import io.github.sk8erboi17.api.pipeline.out.PipelineOut;
import io.github.sk8erboi17.listeners.callbacks.ReceiveData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private static final Listener listener = Listener.getInstance();
    private static final AsynchronousServerSocketChannel serverSocketChannel = AsyncServerSocket.createInput(new InetSocketAddress(8082));
    public static ArrayList<ReceiveData> receiveData = new ArrayList<>(List.of(
            new ReceiverOne(),
            new ReceiverTwo()
    ));

    public static void main(String[] args) throws IOException {

        System.out.println("Server started on port 8082. Waiting for connections...");
        AtomicInteger counter = new AtomicInteger(0);
        listener.startConnectionListen(serverSocketChannel, new ConnectionRequest() {

            @Override
            public void onConnectionAccepted(AsynchronousSocketChannel socketChannel) {
                counter.incrementAndGet();
                System.out.println("(" + counter.get() + ")Accepted connection");
                new PipelineIn(socketChannel, 8192, receiveData.get(counter.get() % 2));
                new PipelineOut(socketChannel).handle(new SendEcho());
            }

            @Override
            public void onConnectionFailed(Throwable exc) {
                System.err.println("Connection failed to be accepted: " + exc.getMessage());
                exc.printStackTrace();
            }

        });

    }
}