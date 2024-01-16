package net.unknown.shared.messaging;

public interface MessageListener<T> {
    void onMessage(T message);
}
