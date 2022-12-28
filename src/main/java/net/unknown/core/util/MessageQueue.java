package net.unknown.core.util;

import net.minecraft.network.chat.Component;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Set;
import java.util.UUID;

public class MessageQueue {
    public interface QueuedMessage {
        long getQueuedTime();
        MessageType getMessageType();
        String getMessage();
        void send();
        boolean save(ConfigurationSection section);
    }

    private static abstract class JSONQueuedMessage implements QueuedMessage {
        private long queuedTime = -1;
        private final MessageType messageType = MessageType.JSON;
        private final String json;

        protected JSONQueuedMessage(String json) {
            this.json = json;
        }

        @Override
        public MessageType getMessageType() {
            return this.messageType;
        }

        @Override
        public long getQueuedTime() {
            return this.queuedTime;
        }

        private void setQueuedTime(long queuedTime) {
            this.queuedTime = queuedTime;
        }
    }

    public static class MinecraftQueuedMessage extends JSONQueuedMessage {
        private long queuedTime = -1;

        private MinecraftQueuedMessage(Component message) {
            super(Component.Serializer.toJson(message));

        }
        @Override
        public long getQueuedTime() {
            return 0;
        }

        @Override
        public MessageType getMessageType() {
            return null;
        }

        @Override
        public String getMessage() {
            return null;
        }

        @Override
        public void send() {

        }

        @Override
        public boolean save(ConfigurationSection section) {
            return false;
        }
    }

    public static class AdventureQueuedMessage implements QueuedMessage {
        @Override
        public long getQueuedTime() {
            return 0;
        }

        @Override
        public MessageType getMessageType() {
            return null;
        }

        @Override
        public String getMessage() {
            return null;
        }

        @Override
        public void send() {

        }

        @Override
        public boolean save(ConfigurationSection section) {
            return false;
        }
    }

    public static class StringQueuedMessage implements QueuedMessage {
        @Override
        public long getQueuedTime() {
            return 0;
        }

        @Override
        public MessageType getMessageType() {
            return null;
        }

        @Override
        public String getMessage() {
            return null;
        }

        @Override
        public void send() {

        }

        @Override
        public boolean save(ConfigurationSection section) {
            return false;
        }
    }

    public enum MessageType {
        JSON,
        STRING
    }
}
