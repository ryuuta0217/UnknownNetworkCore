package net.unknown.shared.messaging;

public interface MessageProcessor<S, T> {
    boolean isValidSource(Object obj);
    UNCMessaging.Side getSide();
    T process(S source);
}
