package io.github.sk8erboi17.api.pipeline.out.requests;

import io.github.sk8erboi17.listeners.callbacks.SendData;

public interface Request {
    Object message();

    SendData callback();
}
