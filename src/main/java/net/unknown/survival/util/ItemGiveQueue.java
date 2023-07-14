package net.unknown.survival.util;

import net.kyori.adventure.text.Component;
import net.unknown.core.configurations.ConfigurationBase;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemGiveQueue extends ConfigurationBase implements Listener {
    private static final ItemGiveQueue INSTANCE = new ItemGiveQueue();
    private Map<UUID, Set<ItemStack>> queue;

    private ItemGiveQueue() {
        super("vote-give-item-queue.yml", true, "VoteManager/GiveItemQueue");
    }

    @Override
    public void onLoad() {
        ListenerManager.unregisterListener(this);
        this.queue = new HashMap<>();
        if (this.getConfig().isSet("queues")) {
            ConfigurationSection queuesSection = this.getConfig().getConfigurationSection("queues");
            queuesSection.getKeys(false).forEach(uuidStr -> {
                UUID uuid = UUID.fromString(uuidStr);
                this.queue.put(uuid, new HashSet<>());
                queuesSection.getStringList(uuidStr).forEach(json -> {
                    this.queue.get(uuid).add(MinecraftAdapter.ItemStack.itemStack(MinecraftAdapter.ItemStack.json(json)));
                });
            });
        }
        ListenerManager.registerListener(this);
    }

    public static boolean queue(UUID uuid, ItemStack item) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            boolean inventoryHasSpace = player.getInventory().firstEmpty() != -1;
            if (inventoryHasSpace) {
                player.getInventory().addItem(item).forEach((slot, overflowItem) -> player.getWorld().dropItem(player.getLocation(), overflowItem));
            } else {
                player.getWorld().dropItem(player.getLocation(), item);
            }
            NewMessageUtil.sendMessage(player, Component.empty()
                    .append(Component.text((inventoryHasSpace ? "インベントリに " : "足元に ")))
                    .append(item.displayName())
                    .appendSpace()
                    .append(Component.text(" ✖" + item.getAmount()))
                    .appendSpace()
                    .append(Component.text((inventoryHasSpace ? "が追加されました" : "が落とされました"))));
            return true;
        }

        INSTANCE.queue.computeIfAbsent(uuid, k -> new HashSet<>()).add(item);
        RunnableManager.runAsync(INSTANCE::save);
        return false;
    }

    public static Set<ItemStack> get(UUID uuid) {
        return INSTANCE.queue.getOrDefault(uuid, Collections.emptySet());
    }

    public static Set<ItemStack> remove(UUID uuid) {
        Set<ItemStack> removed = INSTANCE.queue.remove(uuid);
        if (removed != null) RunnableManager.runAsync(INSTANCE::save);
        return removed;
    }

    @Override
    public synchronized void save() {
        this.getConfig().set("queues", null);
        ConfigurationSection queuesSection = this.getConfig().createSection("queues");
        this.queue.forEach((uuid, items) -> {
            queuesSection.set(uuid.toString(), items.stream()
                    .map(MinecraftAdapter.ItemStack::itemStack)
                    .map(MinecraftAdapter.ItemStack::json)
                    .toList());
        });
        super.save();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ItemGiveQueue.get(event.getPlayer().getUniqueId()).removeIf(queuedItem -> ItemGiveQueue.queue(event.getPlayer().getUniqueId(), queuedItem));
    }

    public static ItemGiveQueue getInstance() {
        return INSTANCE;
    }
}
