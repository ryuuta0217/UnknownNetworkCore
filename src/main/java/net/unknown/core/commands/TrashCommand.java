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

package net.unknown.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.TrashManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrashCommand {
    private static final Component INVENTORY_TITLE = Component.text("ゴミ箱", DefinedTextColor.RED);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("trash");
        builder.executes(ctx -> {
            if(ctx.getSource().getBukkitEntity() instanceof Player player) {
                Inventory bukkitInventory = Bukkit.createInventory(player, 54, INVENTORY_TITLE);
                Container minecraftInventory = MinecraftAdapter.container(bukkitInventory);
                if(minecraftInventory != null) {
                    TrashManager.getItems(player.getUniqueId()).forEach(minecraftInventory::setItem);
                } else {
                    TrashManager.getItemsBukkit(player.getUniqueId()).forEach(bukkitInventory::setItem);
                }
                player.openInventory(bukkitInventory);

                Listener dummy = new Listener() {};

                Bukkit.getPluginManager().registerEvent(InventoryCloseEvent.class, dummy, EventPriority.MONITOR, (l, e) -> {
                    if (e instanceof InventoryCloseEvent event) {
                        if(event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                            if(event.getView().title().contains(INVENTORY_TITLE)) {
                                if(event.getInventory().equals(bukkitInventory)) {
                                    Container nmsInv = MinecraftAdapter.container(event.getInventory());
                                    TrashManager.setItems(player.getUniqueId(), new ArrayList<>(nmsInv.getContents()));
                                    HandlerList.unregisterAll(dummy);
                                }
                            }
                        }
                    }
                }, UnknownNetworkCore.getInstance());
            }
            return 0;
        }).then(Commands.literal("clear")
                .executes(ctx -> {
                    if(ctx.getSource().getBukkitEntity() instanceof Player player) {
                        Map<Integer, ItemStack> nmsItems = TrashManager.getItems(player.getUniqueId());
                        if(nmsItems.values().stream().anyMatch(is -> !is.is(Items.AIR))) {
                            TrashManager.clear(player.getUniqueId());
                            NewMessageUtil.sendMessage(ctx.getSource(), "ゴミ箱からすべてのアイテムを削除しました");
                        } else {
                            NewMessageUtil.sendErrorMessage(ctx.getSource(), "アイテムが何も入っていません");
                        }
                    }
                    return 0;
                }));

        dispatcher.register(builder);
    }
}
