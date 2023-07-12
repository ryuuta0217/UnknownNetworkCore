package net.unknown.survival.dependency;

import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CoreProtect {
    public static boolean isEnabled() {
        return isPluginEnabled() && isAPIEnabled();
    }

    public static boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("CoreProtect");
    }

    public static boolean isAPIEnabled() {
        return net.coreprotect.CoreProtect.getInstance().getAPI() != null && net.coreprotect.CoreProtect.getInstance().getAPI().isEnabled();
    }

    public static CoreProtectAPI getAPI() {
        if (!isEnabled()) throw new IllegalStateException("CoreProtect is not enabled!");
        return net.coreprotect.CoreProtect.getInstance().getAPI();
    }

    public static UUID getLastPlaced(Location location) {
        if (!isEnabled()) throw new IllegalStateException("CoreProtect is not enabled!");
        return getAPI().blockLookup(location.getBlock(), Integer.MAX_VALUE)
                .stream()
                .map(blockChange -> getAPI().parseResult(blockChange))
                .filter(result -> result.getActionId() == 1)
                .findFirst()
                .map(result -> result.getPlayer() != null ? Bukkit.getOfflinePlayer(result.getPlayer()).getUniqueId() : null)
                .orElse(null);
    }
}
