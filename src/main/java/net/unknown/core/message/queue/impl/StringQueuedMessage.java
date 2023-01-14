package net.unknown.core.message.queue.impl;

import net.kyori.adventure.text.Component;
import net.unknown.core.message.queue.MessageType;
import net.unknown.core.message.queue.interfaces.QueuedMessage;
import org.bukkit.configuration.ConfigurationSection;

public class StringQueuedMessage implements QueuedMessage {
    private String message;

    public StringQueuedMessage(String message) {
        this.message = message;
    }

    @Override
    public long getQueuedTime() {
        return 0;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.STRING;
    }

    @Override
    public Component getMessage() {
        return Component.text(this.message);
    }

    @Override
    public boolean save(ConfigurationSection section) {
        try {
            section.set("type", "string");
            section.set("value", this.message);
            return section.isSet("type") && section.isSet("value");
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
