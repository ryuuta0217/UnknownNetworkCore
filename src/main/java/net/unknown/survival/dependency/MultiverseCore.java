package net.unknown.survival.dependency;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class MultiverseCore {
    private static final boolean MULTIVERSE_CORE_ENABLED = Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null && Bukkit.getPluginManager().isPluginEnabled("Multiverse-Core");

    public static boolean isMultiverseCoreEnabled() {
        return MULTIVERSE_CORE_ENABLED;
    }

    public static Location getSpawnLocation(World world) {
        if (!MULTIVERSE_CORE_ENABLED) return world.getSpawnLocation();
        return getInstance().getMVWorldManager().getMVWorld(world).getSpawnLocation();
    }

    public static com.onarandombox.MultiverseCore.MultiverseCore getInstance() {
        return (com.onarandombox.MultiverseCore.MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
    }
}
