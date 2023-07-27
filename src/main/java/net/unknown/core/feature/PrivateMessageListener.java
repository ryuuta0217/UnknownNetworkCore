package net.unknown.core.feature;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.unknown.core.events.PrivateMessageEvent;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PrivateMessageListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrivateMessage(PrivateMessageEvent event) {
        ServerPlayer sender = event.getSource().getPlayer();
        if (sender != null) {
            sender.playNotifySound(SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.5f, 2);
        }

        event.getReceivers().forEach(player -> {
            ServerPlayer minecraftPlayer = MinecraftAdapter.player(player);
            if (minecraftPlayer != null) {
                minecraftPlayer.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1);
            }
        });
    }
}
