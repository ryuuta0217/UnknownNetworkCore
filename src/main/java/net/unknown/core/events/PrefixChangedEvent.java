package net.unknown.core.events;

import net.unknown.core.prefix.Prefix;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PrefixChangedEvent extends Event {
    private final HandlerList handlers = new HandlerList();
    private final UUID player;
    private final @Nullable Prefix oldPrefix;
    private final Prefix newPrefix;

    public PrefixChangedEvent(UUID player, @Nullable Prefix oldPrefix, @Nullable Prefix newPrefix) {
        this.player = player;
        this.oldPrefix = oldPrefix;
        this.newPrefix = newPrefix;
    }

    @Nullable
    public Player getPlayer() {
        if (Bukkit.getOfflinePlayer(this.player).isOnline()) {
            return Bukkit.getPlayer(this.player);
        }

        return null;
    }

    @Nonnull
    public OfflinePlayer getOfflinePlayer() {
        return Bukkit.getOfflinePlayer(this.player);
    }

    @Nullable
    public Prefix getOldPrefix() {
        return this.oldPrefix;
    }

    @Nullable
    public Prefix getNewPrefix() {
        return this.newPrefix;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return new HandlerList();
    }

    @Override
    @Nonnull
    public HandlerList getHandlers() {
        return handlers;
    }
}
