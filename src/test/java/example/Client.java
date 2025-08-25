package example;

import example.requests.SendEcho;
import example.responses.Echo;
import io.github.sk8erboi17.api.output.AsyncClientSocket;
import io.github.sk8erboi17.api.pipeline.in.PipelineIn;
import io.github.sk8erboi17.api.pipeline.out.PipelineOut;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;

public class Client {

    public static void main(String[] args) {
        AsynchronousSocketChannel socketChannel = AsyncClientSocket.createChannel(new InetSocketAddress("localhost", 9090));
        new PipelineIn(socketChannel, 1024 * 4, new Echo());
        PipelineOut pipeline = new PipelineOut(socketChannel);
        pipeline.handle(new SendEcho());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            AsyncClientSocket.closeChannelSocketChannel(socketChannel);
            AsyncClientSocket.shutdown();
        }));
    }
}
