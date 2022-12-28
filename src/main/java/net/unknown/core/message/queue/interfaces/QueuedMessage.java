package net.unknown.core.message.queue.interfaces;

import net.kyori.adventure.text.Component;
import net.unknown.core.message.queue.MessageType;
import org.bukkit.configuration.ConfigurationSection;

public interface QueuedMessage {
    long getQueuedTime();
    MessageType getMessageType();
    Component getMessage();
    boolean save(ConfigurationSection section);
}
