package net.unknown.athletic;

import net.unknown.UnknownNetworkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;

public class Stopwatch extends BukkitRunnable  {
    private static Stopwatch INSTANCE = null;
    private static final NamespacedKey RUNNING_KEY = new NamespacedKey("athletic", "stopwatch_running");
    private static final NamespacedKey TIME_KEY = new NamespacedKey("athletic", "stopwatch_time");

    public static void init() {
        if (INSTANCE != null && !INSTANCE.isCancelled()) {
            INSTANCE.cancel();
            INSTANCE = null;
        }

        INSTANCE = new Stopwatch();
        INSTANCE.runTaskTimerAsynchronously(UnknownNetworkCorePlugin.getInstance(), 0, 1L);
    }

    public static void startStopwatch(Player player) {
        player.getPersistentDataContainer().set(RUNNING_KEY, PersistentDataType.BOOLEAN, true);
    }

    public static void stopStopwatch(Player player) {
        player.getPersistentDataContainer().set(RUNNING_KEY, PersistentDataType.BOOLEAN, false);
    }

    @Override
    public void run() {
        Bukkit.getOnlinePlayers().parallelStream().forEach(player -> {
            PersistentDataContainer dataContainer = player.getPersistentDataContainer();
            if (dataContainer.has(RUNNING_KEY, PersistentDataType.BOOLEAN)) {
                if (Boolean.TRUE.equals(dataContainer.get(RUNNING_KEY, PersistentDataType.BOOLEAN))) {
                    int ticks = 0;
                    if (dataContainer.has(TIME_KEY, PersistentDataType.INTEGER)) {
                        ticks = Optional.ofNullable(dataContainer.get(TIME_KEY, PersistentDataType.INTEGER)).orElse(0);
                    }
                    ticks += 1;
                    dataContainer.set(RUNNING_KEY, PersistentDataType.INTEGER, ticks);
                }
            }
        });
    }
}
