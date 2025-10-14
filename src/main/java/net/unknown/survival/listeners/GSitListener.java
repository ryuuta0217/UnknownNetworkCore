package net.unknown.survival.listeners;

import dev.geco.gsit.api.event.PrePlayerPlayerSitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class GSitListener implements Listener {
    @EventHandler
    public void onPlayerSitPlayer(PrePlayerPlayerSitEvent event) {
        if (event.getTarget().getScoreboardTags().contains("CITIZENS_NPC")) {
            event.setCancelled(true);
        }
    }
}
