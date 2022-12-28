package net.unknown.core.message.queue;

import net.minecraft.network.chat.Component;
import net.unknown.core.message.queue.interfaces.QueuedMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MessageQueue {
    private static final Map<UUID, Map<Long, QueuedMessage>> QUEUED_MESSAGES = new HashMap<>();

    public static void readQueuedMessages() {
        // TODO read from file
    }

    public static Map<Long, QueuedMessage> getQueuedMessages(UUID receiver) {
        return Collections.emptyMap();
    }

    public static synchronized void queue(UUID to, Component message) {
        if (Bukkit.getOfflinePlayer(to).isOnline()) {

        } else {
            synchronized (QUEUED_MESSAGES) {

            }
        }
    }

    public static synchronized void queue(UUID to, net.kyori.adventure.text.Component message) {
        if (Bukkit.getOfflinePlayer(to).isOnline()) {

        } else {
            synchronized (QUEUED_MESSAGES) {

            }
        }
    }

    public static synchronized void queue(UUID to, String message) {
        if (Bukkit.getOfflinePlayer(to).isOnline()) {

        } else {
            synchronized (QUEUED_MESSAGES) {

            }
        }
    }
}
