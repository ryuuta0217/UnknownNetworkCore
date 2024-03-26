package net.unknown.core.fixer;

import com.onarandombox.multiverseinventories.PlayerStats;
import net.unknown.core.dependency.MultiverseCore;
import net.unknown.core.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiverseInventoriesFixer {
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/MultiverseInventoriesFixer");

    public static void fixAll() {
        fixEnderChestMaxSize();
    }

    public static boolean fixEnderChestMaxSize() {
        if (MultiverseCore.isMultiverseCoreEnabled() && Bukkit.getPluginManager().getPlugin("Multiverse-Inventories") != null) {
            try {
                ReflectionUtil.setStaticFinalObject(PlayerStats.class.getDeclaredField("ENDER_CHEST_SIZE"), 9 * 6);
                boolean fixResult = PlayerStats.ENDER_CHEST_SIZE == 9 * 6;
                if (fixResult) LOGGER.info("Fixed Multiverse-Inventories EnderChest size to " + (9 * 6));
                else LOGGER.warn("Failed to fix Multiverse-Inventories EnderChest size!");
                return fixResult;
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
}
