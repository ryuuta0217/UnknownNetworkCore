package net.unknown.survival.discord;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MinecraftToDiscordMessageListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChatEarly(AsyncChatEvent event) {
        event.viewers().add(MinecraftToDiscordMessageListener.DiscordAudienceDummy.INSTANCE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChatFinally(AsyncChatEvent event) {
        if (event.viewers().contains(MinecraftToDiscordMessageListener.DiscordAudienceDummy.INSTANCE)) {

        }
    }

    public static class DiscordAudienceDummy implements Audience {
        public static final DiscordAudienceDummy INSTANCE = new DiscordAudienceDummy();
        private DiscordAudienceDummy() {}
    }
}
