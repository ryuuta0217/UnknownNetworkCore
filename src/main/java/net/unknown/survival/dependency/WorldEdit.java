package net.unknown.survival.dependency;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.RegionSelector;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldEdit {
    public static WorldEditPlugin getInstance() {
        return (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
    }

    public static LocalSession getSession(Player player) {
        return getInstance().getSession(player);
    }

    public static RegionSelector getRegionSelector(Player player, World world) {
        return getSession(player).getRegionSelector(BukkitAdapter.adapt(world));
    }
}
