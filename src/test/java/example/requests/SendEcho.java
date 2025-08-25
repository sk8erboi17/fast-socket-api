package example.requests;

import io.github.sk8erboi17.listeners.callbacks.SendData;
import io.github.sk8erboi17.api.pipeline.out.requests.Request;

public class SendEcho implements Request {
    @Override
    public String message() {
        return "Hi";
    }

    @Override
    public SendData callback() {
        return new SendData() {

            @Override
            public void onSendComplete() {
                System.out.println("Send complete");
            }

            @Override
            public void exception(Throwable throwable) {
                throwable.printStackTrace();
            }
        };
    }
}
