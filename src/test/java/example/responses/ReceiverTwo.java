package example.responses;

import io.github.sk8erboi17.listeners.callbacks.ReceiveData;

public class ReceiverTwo implements ReceiveData {
    @Override
    public void receive(Object data) {
        System.out.println("(2) Received data: " + data);
    }

    @Override
    public void exception(Throwable throwable) {
        throwable.printStackTrace();
    }
}
