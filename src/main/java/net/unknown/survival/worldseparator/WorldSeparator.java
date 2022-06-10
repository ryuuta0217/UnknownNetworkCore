/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
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

package net.unknown.survival.worldseparator;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.*;
import java.util.stream.IntStream;

/**
 * インベントリ、体力、経験値、進捗、統計をワールドごとに分離します。
 * なるべくBukkit APIを使用せず、Minecraft内部クラス、関数を使用すること (挑戦、というより、アイテムNBTとかがJSONで出てくるほうがいいので)
 *
 * インベントリ・・・ ((CraftPlayerInventory) Player#getInventory).getInventory(), ItemStack#getTag
 * 進捗・・・ ServerPlayer#getAdvancements, PlayerAdvancements#save, PlayerAdvancements#load(ServerAdvancementManager)
 */
public class WorldSeparator implements Listener {
    private static final Map<String, Set<String>> WORLD_GROUP = new HashMap<>() {{
        // このグループのワールド間の移動は同一のインベントリ、体力、経験値、進捗、統計を使用(処理不要)
        put("default", new HashSet<>() {{
            add("world");
            add("world_nether");
            add("world_the_end");

            add("main2");
            add("main2_nether");
            add("main2_the_end");

            add("resource");
            add("resource_nether");
            add("resource_the_end");
        }});

        // その他のワールドは全て別々のインベントリ、体力、経験値、進捗、統計
    }};
    private static final Map<String, Map<UUID, List<ItemStack>>> INVENTORIES = new HashMap<>(); // Map<Group, Map<Player, Items>>
    private static final Map<String, Map<UUID, Float>> HEALTH = new HashMap<>();
    private static final Map<String, Map<UUID, Map<Advancement, AdvancementProgress>>> ADVANCEMENTS = new HashMap<>();

    @EventHandler
    public void onWorldChanged(PlayerChangedWorldEvent event) {
        World from = event.getFrom();
        String fromGroup = getWorldGroup(from);
        World to = event.getPlayer().getWorld();
        String toGroup = getWorldGroup(to);
        if(!fromGroup.equals(toGroup)) {
            ServerPlayer player = ((CraftPlayer) event.getPlayer()).getHandle();

            /* SAVE */

            /* INVENTORY */
            Map<UUID, List<ItemStack>> p2item = INVENTORIES.getOrDefault(fromGroup, new HashMap<>());
            // TODO ItemStackの同一オブジェクト参照状態・要修正 (修正済)
            List<ItemStack> currentItems = player.getInventory().getContents();
            List<ItemStack> toSaveItems = currentItems.stream()
                    .map(ItemStack::copy)
                    .toList();
            p2item.put(event.getPlayer().getUniqueId(), toSaveItems);
            INVENTORIES.put(fromGroup, p2item);

            /* ADVANCEMENTS */
            //player.getAdvancements().reload();

            /* HEALTH */
            Map<UUID, Float> p2health = HEALTH.getOrDefault(fromGroup, new HashMap<>());
            p2health.put(player.getUUID(), player.getHealth());
            HEALTH.put(fromGroup, p2health);

            /* LOAD */

            player.getInventory().clearContent();
            if(INVENTORIES.containsKey(toGroup) && INVENTORIES.get(toGroup).containsKey(player.getUUID())) {
                List<ItemStack> items = INVENTORIES.get(toGroup).get(player.getUUID());
                IntStream.range(0, items.size()).forEach(i -> player.getInventory().setItem(i, items.get(i).copy()));
            }

            player.setHealth(HEALTH.getOrDefault(toGroup, new HashMap<>()).getOrDefault(player.getUUID(), 20F));
        }
    }

    public static String getWorldGroup(World world) {
        return WORLD_GROUP.entrySet()
                .stream()
                .filter(e -> e.getValue().contains(world.getName()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(world.getName());
    }
}
