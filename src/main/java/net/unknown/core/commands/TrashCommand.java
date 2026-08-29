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

package net.unknown.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.enums.Permissions;
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
import net.unknown.survival.feature.redirector.trash.TrashOverflowBehavior;
import net.unknown.survival.feature.redirector.trash.TrashRedirector;
import net.unknown.survival.feature.redirector.trash.TrashRedirectorMode;

import java.util.ArrayList;
import java.util.Map;

public class TrashCommand {
    private static final Component INVENTORY_TITLE = Component.text("ゴミ箱", DefinedTextColor.RED);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("trash");
        builder.requires(Permissions.COMMAND_TRASH::checkAndIsPlayer);
        builder.executes(ctx -> {
            if (ctx.getSource().getBukkitEntity() instanceof Player player) {
                Inventory bukkitInventory = Bukkit.createInventory(player, 54, INVENTORY_TITLE);
                Container minecraftInventory = MinecraftAdapter.container(bukkitInventory);
                if (minecraftInventory != null) {
                    TrashManager.getItems(player.getUniqueId()).forEach(minecraftInventory::setItem);
                } else {
                    TrashManager.getItemsBukkit(player.getUniqueId()).forEach(bukkitInventory::setItem);
                }
                player.openInventory(bukkitInventory);

                Listener dummy = new Listener() {
                };

                Bukkit.getPluginManager().registerEvent(InventoryCloseEvent.class, dummy, EventPriority.MONITOR, (l, e) -> {
                    if (e instanceof InventoryCloseEvent event) {
                        if (event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                            if (event.getView().title().contains(INVENTORY_TITLE)) {
                                if (event.getInventory().equals(bukkitInventory)) {
                                    Container nmsInv = MinecraftAdapter.container(event.getInventory());
                                    TrashManager.setItems(player.getUniqueId(), new ArrayList<>(nmsInv.getContents()));
                                    HandlerList.unregisterAll(dummy);
                                }
                            }
                        }
                    }
                }, UnknownNetworkCorePlugin.getInstance());
            }
            return 0;
        }).then(Commands.literal("clear")
                .executes(ctx -> {
                    if (ctx.getSource().getBukkitEntity() instanceof Player player) {
                        Map<Integer, ItemStack> nmsItems = TrashManager.getItems(player.getUniqueId());
                        if (nmsItems.values().stream().anyMatch(is -> !is.is(Items.AIR))) {
                            TrashManager.clear(player.getUniqueId());
                            NewMessageUtil.sendMessage(ctx.getSource(), "ゴミ箱からすべてのアイテムを削除しました");
                        } else {
                            NewMessageUtil.sendErrorMessage(ctx.getSource(), "アイテムが何も入っていません");
                        }
                    }
                    return 0;
                }));

        LiteralArgumentBuilder<CommandSourceStack> redirectorNode = Commands.literal("redirector")
                .executes(TrashCommand::showRedirectorInfo);

        LiteralArgumentBuilder<CommandSourceStack> modeNode = Commands.literal("mode")
                .executes(TrashCommand::showRedirectorMode);
        for (TrashRedirectorMode mode : TrashRedirectorMode.values()) {
            modeNode.then(Commands.literal(mode.name())
                    .executes(ctx -> setRedirectorMode(ctx, mode)));
        }
        redirectorNode.then(modeNode);

        LiteralArgumentBuilder<CommandSourceStack> overflowNode = Commands.literal("overflow")
                .executes(TrashCommand::showOverflowBehavior);
        for (TrashOverflowBehavior behavior : TrashOverflowBehavior.values()) {
            overflowNode.then(Commands.literal(behavior.name())
                    .executes(ctx -> setOverflowBehavior(ctx, behavior)));
        }
        redirectorNode.then(overflowNode);

        builder.then(redirectorNode)
               .then(modeNode)
               .then(overflowNode);

        dispatcher.register(builder);
    }

    private static int showRedirectorInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        TrashRedirectorMode mode = TrashRedirector.getMode(player);
        TrashOverflowBehavior overflow = TrashRedirector.getOverflowBehavior(player);

        Component msg = Component.text("--- [ゴミ箱自動転送設定] ---", DefinedTextColor.RED)
                .append(Component.newline())
                .append(Component.text("現在の転送モード: ", DefinedTextColor.WHITE)).append(mode.getDescription())
                .append(Component.newline())
                .append(Component.text("満杯時の動作: ", DefinedTextColor.WHITE)).append(overflow.getDescription());
        NewMessageUtil.sendMessage(ctx.getSource(), msg);
        return 0;
    }

    private static int showRedirectorMode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        TrashRedirectorMode mode = TrashRedirector.getMode(player);
        NewMessageUtil.sendMessage(ctx.getSource(),mode.getDescription());
        return 0;
    }

    private static int setRedirectorMode(CommandContext<CommandSourceStack> ctx, TrashRedirectorMode mode) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        TrashRedirector.setMode(player, mode);
        NewMessageUtil.sendMessage(ctx.getSource(), mode.getModeChangedMessage());
        return 0;
    }

    private static int showOverflowBehavior(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        TrashOverflowBehavior behavior = TrashRedirector.getOverflowBehavior(player);
        NewMessageUtil.sendMessage(ctx.getSource(), behavior.getDescription());
        return 0;
    }

    private static int setOverflowBehavior(CommandContext<CommandSourceStack> ctx, TrashOverflowBehavior behavior) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        TrashRedirector.setOverflowBehavior(player, behavior);
        NewMessageUtil.sendMessage(ctx.getSource(), behavior.getModeChangedMessage());
        return 0;
    }
}
