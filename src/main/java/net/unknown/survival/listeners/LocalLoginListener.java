package net.unknown.survival.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class LocalLoginListener implements Listener {
    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getAddress().toString().contains("127.0.0.1")) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, Component.text("無効な場所からのログインです。再試行してください。"));
        }
    }
}
