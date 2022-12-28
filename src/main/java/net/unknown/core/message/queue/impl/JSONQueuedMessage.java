package net.unknown.core.message.queue.impl;

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.chat.Component;
import net.unknown.core.message.queue.MessageType;
import net.unknown.core.message.queue.interfaces.QueuedMessage;
import org.bukkit.configuration.ConfigurationSection;

public class JSONQueuedMessage implements QueuedMessage {
    private long queuedTime = -1;
    private final MessageType messageType = MessageType.JSON;
    private final String json;

    protected JSONQueuedMessage(String json) {
        this.json = json;
    }

    public JSONQueuedMessage(Component minecraft) {
        this.json = Component.Serializer.toJson(minecraft);
    }

    public JSONQueuedMessage(net.kyori.adventure.text.Component adventure) {
        this.json = GsonComponentSerializer.gson().serialize(adventure);
    }

    @Override
    public MessageType getMessageType() {
        return this.messageType;
    }

    @Override
    public net.kyori.adventure.text.Component getMessage() {
        return GsonComponentSerializer.gson().deserialize(this.json);
    }

    @Override
    public long getQueuedTime() {
        return this.queuedTime;
    }

    @Override
    public boolean save(ConfigurationSection section) {
        try {
            section.set("type", "json");
            section.set("value", this.json);
            return section.isSet("type") && section.isSet("value");
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getJson() {
        return this.json;
    }

    private void setQueuedTime(long queuedTime) {
        this.queuedTime = queuedTime;
    }
}
