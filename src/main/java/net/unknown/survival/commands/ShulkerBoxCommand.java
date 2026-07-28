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

package net.unknown.survival.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.enums.Permissions;
import net.unknown.survival.feature.OpenShulkerBoxInHand;
import net.unknown.survival.feature.redirector.ShulkerBoxRedirector;
import net.unknown.survival.feature.redirector.ShulkerBoxRedirectorMode;
import net.unknown.survival.feature.redirector.ShulkerBoxSearchOrder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

// /<shulkerbox|sb> <how-open> <click|inventory> <mode>
public class ShulkerBoxCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("shulkerbox");
        builder.requires(Permissions.COMMAND_SHULKERBOX::checkAndIsPlayer);

        LiteralArgumentBuilder<CommandSourceStack> clickNode = Commands.literal("click")
                .executes(ShulkerBoxCommand::showClickOpenMode);
        for (OpenShulkerBoxInHand.OpenMode mode : OpenShulkerBoxInHand.OpenMode.values()) {
            clickNode.then(Commands.literal(mode.name())
                    .executes(ctx -> setClickOpenMode(ctx, mode)));
        }

        LiteralArgumentBuilder<CommandSourceStack> inventoryNode = Commands.literal("inventory")
                .executes(ShulkerBoxCommand::showInventoryOpenMode);
        for (OpenShulkerBoxInHand.InventoryOpenMode mode : OpenShulkerBoxInHand.InventoryOpenMode.values()) {
            inventoryNode.then(Commands.literal(mode.name())
                    .executes(ctx -> setInventoryOpenMode(ctx, mode)));
        }

        LiteralArgumentBuilder<CommandSourceStack> redirectorNode = Commands.literal("redirector")
                .requires(Permissions.FEATURE_SHULKER_BOX_REDIRECTOR::checkAndIsPlayer)
                .executes(ShulkerBoxCommand::showRedirectorInfo);

        LiteralArgumentBuilder<CommandSourceStack> modeNode = Commands.literal("mode")
                .executes(ShulkerBoxCommand::showRedirectorMode);
        for (ShulkerBoxRedirectorMode mode : ShulkerBoxRedirectorMode.values()) {
            modeNode.then(Commands.literal(mode.name())
                    .executes(ctx -> setRedirectorMode(ctx, mode)));
        }
        redirectorNode.then(modeNode);

        LiteralArgumentBuilder<CommandSourceStack> orderNode = Commands.literal("order")
                .executes(ShulkerBoxCommand::showRedirectorOrder);
        for (ShulkerBoxSearchOrder order : ShulkerBoxSearchOrder.values()) {
            orderNode.then(Commands.literal(order.name())
                    .executes(ctx -> setRedirectorOrder(ctx, order)));
        }
        redirectorNode.then(orderNode);

        LiteralArgumentBuilder<CommandSourceStack> markersNode = Commands.literal("markers")
                .executes(ShulkerBoxCommand::listMarkers)
                .then(Commands.literal("list").executes(ShulkerBoxCommand::listMarkers))
                .then(Commands.literal("add")
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(ctx -> addMarker(ctx, ItemArgument.getItem(ctx, "item")))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .suggests((ctx, suggestionsBuilder) -> {
                                    Set<NamespacedKey> markers = ShulkerBoxRedirector.getMarkers(ctx.getSource().getPlayerOrException().getBukkitEntity().getInventory().getItemInMainHand());
                                    return SharedSuggestionProvider.suggestResource(markers.parallelStream().map(CraftNamespacedKey::toMinecraft), suggestionsBuilder);
                                })
                                .executes(ctx -> removeMarker(ctx, ItemArgument.getItem(ctx, "item")))))
                .then(Commands.literal("clear")
                        .executes(ShulkerBoxCommand::clearMarkers));
        redirectorNode.then(markersNode);

        builder.then(Commands.literal("how-open")
                        .then(clickNode)
                        .then(inventoryNode)
                        .executes(ctx -> showClickOpenMode(ctx) + showInventoryOpenMode(ctx)))
                .then(redirectorNode);

        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(builder);

        LiteralArgumentBuilder<CommandSourceStack> aliasBuilder = Commands.literal("sb")
                        .requires(Permissions.COMMAND_SHULKERBOX::checkAndIsPlayer)
                                .redirect(node);

        dispatcher.register(aliasBuilder);
    }

    private static int showClickOpenMode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        OpenShulkerBoxInHand.OpenMode mode = OpenShulkerBoxInHand.getOpenMode(ctx.getSource().getPlayerOrException().getUUID());
        NewMessageUtil.sendMessage(ctx.getSource(), mode.getDescription());
        return 0;
    }

    private static int showInventoryOpenMode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        OpenShulkerBoxInHand.InventoryOpenMode mode = OpenShulkerBoxInHand.getInventoryOpenMode(ctx.getSource().getPlayerOrException().getUUID());
        NewMessageUtil.sendMessage(ctx.getSource(), mode.getDescription());
        return 0;
    }

    private static int setClickOpenMode(CommandContext<CommandSourceStack> ctx, OpenShulkerBoxInHand.OpenMode mode) throws CommandSyntaxException {
        OpenShulkerBoxInHand.setOpenMode(ctx.getSource().getPlayerOrException().getUUID(), mode);
        NewMessageUtil.sendMessage(ctx.getSource(), mode.getModeChangedMessage());
        return 0;
    }

    private static int setInventoryOpenMode(CommandContext<CommandSourceStack> ctx, OpenShulkerBoxInHand.InventoryOpenMode mode) throws CommandSyntaxException {
        OpenShulkerBoxInHand.setInventoryOpenMode(ctx.getSource().getPlayerOrException().getUUID(), mode);
        NewMessageUtil.sendMessage(ctx.getSource(), mode.getModeChangedMessage());
        return 0;
    }

    private static ItemStack getTargetShulkerBox(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && Tag.SHULKER_BOXES.isTagged(mainHand.getType())) {
            return mainHand;
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && Tag.SHULKER_BOXES.isTagged(offHand.getType())) {
            return offHand;
        }
        return null;
    }

    private static int showRedirectorInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        ShulkerBoxRedirectorMode mode = ShulkerBoxRedirector.getMode(player);
        ShulkerBoxSearchOrder order = ShulkerBoxRedirector.getSearchOrder(player);

        Component msg = Component.text("--- [自動回収設定 (Redirector)] ---", DefinedTextColor.GOLD)
                .append(Component.newline())
                .append(Component.text("現在のモード: ", DefinedTextColor.WHITE)).append(mode.getDescription())
                .append(Component.newline())
                .append(Component.text("検索順序: ", DefinedTextColor.WHITE)).append(order.getDescription());

        ItemStack box = getTargetShulkerBox(player);
        if (box != null) {
            Set<NamespacedKey> markers = ShulkerBoxRedirector.getMarkers(box);
            msg = msg.append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("--- [手に持っているボックスのマーカー] ---", DefinedTextColor.AQUA))
                    .append(Component.newline())
                    .append(Component.text("登録数: ", DefinedTextColor.WHITE)).append(Component.text(markers.size() + "件", DefinedTextColor.YELLOW));
            if (!markers.isEmpty()) {
                for (NamespacedKey key : markers) {
                    Material mat = Material.matchMaterial(key.toString());
                    if (mat == null && key.getKey() != null) mat = Material.matchMaterial(key.getKey());
                    if (mat != null) {
                        msg = msg.append(Component.newline())
                                .append(Component.text(" ▸ ", DefinedTextColor.YELLOW))
                                .append(Component.translatable(mat).color(DefinedTextColor.GREEN));
                    } else {
                        msg = msg.append(Component.newline())
                                .append(Component.text(" ▸ ", DefinedTextColor.YELLOW))
                                .append(Component.text(key.toString(), DefinedTextColor.GREEN));
                    }
                }
            }
        }

        NewMessageUtil.sendMessage(ctx.getSource(), msg);
        return 0;
    }

    private static int showRedirectorMode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        ShulkerBoxRedirectorMode mode = ShulkerBoxRedirector.getMode(player);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.text("現在の自動回収モード: ", DefinedTextColor.WHITE).append(mode.getDescription()));
        return 0;
    }

    private static int setRedirectorMode(CommandContext<CommandSourceStack> ctx, ShulkerBoxRedirectorMode mode) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        ShulkerBoxRedirector.setMode(player, mode);
        NewMessageUtil.sendMessage(ctx.getSource(), mode.getModeChangedMessage());
        return 0;
    }

    private static int showRedirectorOrder(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        ShulkerBoxSearchOrder order = ShulkerBoxRedirector.getSearchOrder(player);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.text("現在の検索順序: ", DefinedTextColor.WHITE).append(order.getDescription()));
        return 0;
    }

    private static int setRedirectorOrder(CommandContext<CommandSourceStack> ctx, ShulkerBoxSearchOrder order) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        ShulkerBoxRedirector.setSearchOrder(player, order);
        NewMessageUtil.sendMessage(ctx.getSource(), order.getModeChangedMessage());
        return 0;
    }

    private static int listMarkers(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        ItemStack box = getTargetShulkerBox(player);
        if (box == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "手にシュルカーボックスを持っていません");
            return -1;
        }

        Set<NamespacedKey> markers = ShulkerBoxRedirector.getMarkers(box);
        if (markers.isEmpty()) {
            NewMessageUtil.sendMessage(ctx.getSource(), Component.text("このシュルカーボックスにはマーカーが設定されていません。", DefinedTextColor.YELLOW));
            return 0;
        }

        Component msg = Component.text("--- [ボックスに設定されたマーカー一覧 (" + markers.size() + "件)] ---", DefinedTextColor.AQUA);
        for (NamespacedKey key : markers) {
            Material mat = Material.matchMaterial(key.toString());
            if (mat == null && key.getKey() != null) mat = Material.matchMaterial(key.getKey());
            if (mat != null) {
                msg = msg.append(Component.newline())
                        .append(Component.text(" ▸ ", DefinedTextColor.YELLOW))
                        .append(Component.translatable(mat).color(DefinedTextColor.GREEN));
            } else {
                msg = msg.append(Component.newline())
                        .append(Component.text(" ▸ ", DefinedTextColor.YELLOW))
                        .append(Component.text(key.toString(), DefinedTextColor.GREEN));
            }
        }
        NewMessageUtil.sendMessage(ctx.getSource(), msg);
        return 0;
    }

    private static int addMarker(CommandContext<CommandSourceStack> ctx, ItemInput itemInput) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        ItemStack box = getTargetShulkerBox(player);
        if (box == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "手にシュルカーボックスを持っていません");
            return -1;
        }

        ItemStack tempItem = MinecraftAdapter.ItemStack.itemStack(itemInput.createItemStack(1));
        Material mat = tempItem.getType();
        if (mat == null || mat.isAir()) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "無効なアイテムです");
            return -1;
        }

        if (ShulkerBoxRedirector.getMarkers(box).contains(mat.getKey())) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.translatable(mat).color(DefinedTextColor.RED).append(Component.text(" は既にマークされています", DefinedTextColor.RED)));
            return -1;
        }

        ShulkerBoxRedirector.addMarker(box, mat);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.translatable(mat).color(DefinedTextColor.GREEN).append(Component.text(" をマーカーに追加しました")));
        return 0;
    }

    private static int removeMarker(CommandContext<CommandSourceStack> ctx, ItemInput itemInput) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        ItemStack box = getTargetShulkerBox(player);
        if (box == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "手にシュルカーボックスを持っていません");
            return -1;
        }

        ItemStack tempItem = MinecraftAdapter.ItemStack.itemStack(itemInput.createItemStack(1));
        Material mat = tempItem.getType();

        if (!ShulkerBoxRedirector.getMarkers(box).contains(mat.getKey())) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.translatable(mat).color(DefinedTextColor.RED).append(Component.text(" はマークされていません", DefinedTextColor.RED)));
            return -1;
        }

        ShulkerBoxRedirector.removeMarker(box, mat);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.translatable(mat).color(DefinedTextColor.YELLOW).append(Component.text(" をマーカーから削除しました")));
        return 0;
    }

    private static int clearMarkers(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        ItemStack box = getTargetShulkerBox(player);
        if (box == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "手にシュルカーボックスを持っていません");
            return -1;
        }

        Set<NamespacedKey> markers = ShulkerBoxRedirector.getMarkers(box);
        if (markers.isEmpty()) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "マーカーは設定されていません");
            return -1;
        }

        ShulkerBoxRedirector.clearMarkers(box);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.text("登録されていた " + markers.size() + "件 のマーカーをすべてクリアしました", DefinedTextColor.GREEN));
        return 0;
    }
}
