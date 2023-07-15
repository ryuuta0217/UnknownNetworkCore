/*
 * Copyright (c) 2023 Unknown Network Developers and contributors.
 *
 * All rights reserved.
 *
 * NOTICE: This license is subject to change without prior notice.
 *
 * Redistribution and use in source and binary forms, *without modification*,
 *     are permitted provided that the following conditions are met:
 *
 * I. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 * II. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * III. Neither the name of Unknown Network nor the names of its contributors may be used to
 *     endorse or promote products derived from this software without specific prior written permission.
 *
 * IV. This source code and binaries is provided by the copyright holders and contributors "AS-IS" and
 *     any express or implied warranties, including, but not limited to, the implied warranties of
 *     merchantability and fitness for a particular purpose are disclaimed.
 *     In not event shall the copyright owner or contributors be liable for
 *     any direct, indirect, incidental, special, exemplary, or consequential damages
 *     (including but not limited to procurement of substitute goods or services;
 *     loss of use data or profits; or business interruption) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

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
        super("vote-give-item-queue.yml", false, "VoteManager/GiveItemQueue");
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
                    .append(Component.text(" x" + item.getAmount()))
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