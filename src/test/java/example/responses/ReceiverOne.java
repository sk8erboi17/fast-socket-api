package example.responses;

import io.github.sk8erboi17.listeners.callbacks.ReceiveData;

import java.time.Instant;

public class ReceiverOne implements ReceiveData {
    @Override
    public void receive(Object data) {
        System.out.println(Instant.now().toString()  +  " - (1) Received data: " + data);
    }

    @Override
    public void exception(Throwable throwable) {
        throwable.printStackTrace();
    }
}
