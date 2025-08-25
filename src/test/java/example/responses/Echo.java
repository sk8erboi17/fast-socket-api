package example.responses;

import io.github.sk8erboi17.listeners.callbacks.ReceiveData;

public class Echo implements ReceiveData {
    @Override
    public void receive(Object data) {
        System.out.println(data);
    }

    @Override
    public void exception(Throwable throwable) {
        throwable.printStackTrace();
    }
}
