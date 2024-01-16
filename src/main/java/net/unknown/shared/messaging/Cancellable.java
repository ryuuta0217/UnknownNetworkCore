package net.unknown.shared.messaging;

public interface Cancellable {
    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
