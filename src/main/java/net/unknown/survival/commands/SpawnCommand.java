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
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.dependency.MultiverseCore;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.data.Spawns;
import net.unknown.survival.data.Villages;
import net.unknown.survival.data.model.Spawn;
import net.unknown.survival.data.model.Village;
import net.unknown.survival.enums.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SpawnCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("spawn");
        builder.requires(Permissions.COMMAND_SPAWN::check);

        // /spawn
        builder.executes(SpawnCommand::execTeleport);

        // /spawn set <identifier>
        builder.then(Commands.literal("set")
                .requires(Permissions.COMMAND_SPAWN::checkAndIsPlayer)
                .then(Commands.argument("identifier", IdentifierArgument.id())
                        .suggests((ctx, suggestionsBuilder) -> {
                            List<Identifier> suggestions = new ArrayList<>();
                            suggestions.add(PlayerData.SpawnConfigData.getDefaultIdentifier());
                            suggestions.addAll(Spawns.getSpawns().keySet());
                            Villages.getVillages().values().forEach(village ->
                                    suggestions.add(Identifier.parse("unknown-network:villages/" + village.getName())));
                            return SharedSuggestionProvider.suggestResource(suggestions, suggestionsBuilder);
                        })
                        .executes(SpawnCommand::execSet)));

        // /spawn override <true|false>
        builder.then(Commands.literal("override")
                .requires(Permissions.COMMAND_SPAWN::checkAndIsPlayer)
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(SpawnCommand::execOverride)));

        // /spawn info
        builder.then(Commands.literal("info")
                .requires(Permissions.COMMAND_SPAWN::checkAndIsPlayer)
                .executes(SpawnCommand::execInfo));

        // /spawn list
        builder.then(Commands.literal("list")
                .executes(SpawnCommand::execList));

        // /spawn add <識別子> <名前> <表示名> <説明> <座標> [向き]
        builder.then(Commands.literal("add")
                .requires(Permissions.COMMAND_SPAWN_MANAGE::check)
                .then(Commands.argument("識別子", IdentifierArgument.id())
                        .then(Commands.argument("名前", StringArgumentType.string())
                                .then(Commands.argument("アイコン", ItemArgument.item(buildContext))
                                        .then(Commands.argument("表示名", ComponentArgument.textComponent(buildContext))
                                                .then(Commands.argument("説明", ComponentArgument.textComponent(buildContext))
                                                        .then(Commands.argument("座標", Vec3Argument.vec3(true))
                                                                .executes(SpawnCommand::execAdd)
                                                                .then(Commands.argument("向き", RotationArgument.rotation())
                                                                        .executes(SpawnCommand::execAdd)))))))));

        // /spawn remove <識別子>
        builder.then(Commands.literal("remove")
                .requires(Permissions.COMMAND_SPAWN_MANAGE::check)
                .then(Commands.argument("識別子", IdentifierArgument.id())
                        .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggestResource(Spawns.getSpawns().keySet(), suggestionsBuilder))
                        .executes(SpawnCommand::execRemove)));

        dispatcher.register(builder);
    }

    private static int execTeleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        net.minecraft.world.entity.Entity executor = ctx.getSource().getEntityOrException();

        Location destination;
        if (executor instanceof net.minecraft.world.entity.player.Player player) {
            PlayerData.SpawnConfigData config = PlayerData.of(player.getUUID()).getSpawnConfigData();
            destination = config.resolveLocation();
        } else {
            destination = null;
        }

        // フォールバック: メインワールドスポーン
        if (destination == null) {
            org.bukkit.World world = Bukkit.getWorld("world");
            if (world != null) {
                destination = MultiverseCore.getSpawnLocation(world);
            }
        }

        if (destination == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "スポーン地点を解決できませんでした。");
            return 0;
        }

        executor.teleportTo(
                ((org.bukkit.craftbukkit.CraftWorld) destination.getWorld()).getHandle(),
                destination.getX(), destination.getY(), destination.getZ(),
                Set.of(), destination.getYaw(), destination.getPitch(),
                false, PlayerTeleportEvent.TeleportCause.COMMAND);

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("スポーン地点にテレポートしました")));
        return 1;
    }

    private static int execSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier identifier = IdentifierArgument.getId(ctx, "identifier");
        net.minecraft.world.entity.player.Player player = ctx.getSource().getPlayerOrException();
        PlayerData.SpawnConfigData config = PlayerData.of(player.getUUID()).getSpawnConfigData();

        config.setSpawnIdentifier(identifier);

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("スポーン地点を ", DefinedTextColor.GREEN))
                .append(Component.text(identifier.toString(), DefinedTextColor.AQUA))
                .append(Component.text(" に設定しました。", DefinedTextColor.GREEN)));
        return 1;
    }

    private static int execOverride(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
        net.minecraft.world.entity.player.Player player = ctx.getSource().getPlayerOrException();
        PlayerData.SpawnConfigData config = PlayerData.of(player.getUUID()).getSpawnConfigData();

        config.setOverrideRespawn(enabled);

        if (enabled) {
            NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                    .append(Component.text("リスポーン地点の上書きを ", DefinedTextColor.GREEN))
                    .append(Component.text("有効", DefinedTextColor.AQUA))
                    .append(Component.text(" にしました。ベッドでのリスポーン地点設定は無視されます。", DefinedTextColor.GREEN)));
        } else {
            NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                    .append(Component.text("リスポーン地点の上書きを ", DefinedTextColor.GREEN))
                    .append(Component.text("無効", DefinedTextColor.AQUA))
                    .append(Component.text(" にしました。ベッドでリスポーン地点が設定されている場合はそちらが優先されます。", DefinedTextColor.GREEN)));
        }
        return 1;
    }

    private static int execInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        net.minecraft.world.entity.player.Player player = ctx.getSource().getPlayerOrException();
        PlayerData.SpawnConfigData config = PlayerData.of(player.getUUID()).getSpawnConfigData();

        Component message = Component.empty()
                .append(Component.text("=== スポーン設定 ===", DefinedTextColor.GREEN))
                .appendNewline()
                .append(Component.text("スポーン先: ", DefinedTextColor.GRAY))
                .append(Component.text(config.getSpawnIdentifier().toString(), DefinedTextColor.AQUA))
                .appendNewline()
                .append(Component.text("リスポーン上書き: ", DefinedTextColor.GRAY))
                .append(config.isOverrideRespawn()
                        ? Component.text("有効", DefinedTextColor.RED)
                        : Component.text("無効", DefinedTextColor.GREEN));

        // 解決先の表示
        Location resolved = config.resolveLocation();
        if (resolved != null && resolved.getWorld() != null) {
            message = message.appendNewline()
                    .append(Component.text("解決先: ", DefinedTextColor.GRAY))
                    .append(Component.text(String.format("%s, %.1f, %.1f, %.1f",
                            MessageUtil.getWorldName(resolved.getWorld()),
                            resolved.getX(), resolved.getY(), resolved.getZ()), DefinedTextColor.WHITE));
        } else {
            message = message.appendNewline()
                    .append(Component.text("解決先: ", DefinedTextColor.GRAY))
                    .append(Component.text("解決不可（フォールバックが使用されます）", DefinedTextColor.YELLOW));
        }

        NewMessageUtil.sendMessage(ctx.getSource(), message, false);
        return 1;
    }

    private static int execList(CommandContext<CommandSourceStack> ctx) {
        Collection<Spawn> spawns = Spawns.getSpawns().values();
        Collection<Village> villages = Villages.getVillages().values();

        if (spawns.isEmpty() && villages.isEmpty()) {
            NewMessageUtil.sendMessage(ctx.getSource(), "登録済みのスポーン地点はありません。");
            return 0;
        }

        Component header = Component.text("=== スポーン地点一覧 ===", DefinedTextColor.GREEN);
        Component message = Component.empty().append(header);

        // デフォルト
        message = message.appendNewline()
                .append(Component.text("  unknown-network:default", DefinedTextColor.AQUA))
                .append(Component.text(" - ", DefinedTextColor.GRAY))
                .append(Component.text("メインワールドスポーン", DefinedTextColor.WHITE));

        // カスタムスポーン
        if (!spawns.isEmpty()) {
            message = message.appendNewline()
                    .append(Component.text("--- カスタムスポーン (" + spawns.size() + ") ---", DefinedTextColor.GOLD));
            for (Spawn spawn : spawns) {
                message = message.appendNewline()
                        .append(Component.text("  " + spawn.getIdentifier().toString(), DefinedTextColor.AQUA))
                        .append(Component.text(" - ", DefinedTextColor.GRAY))
                        .append(Component.text(spawn.getName()).hoverEvent(HoverEvent.showText(spawn.getDescription())))
                        .append(Component.text(" - ", DefinedTextColor.GRAY))
                        .append(Component.text(String.format("%s, %.1f, %.1f, %.1f",
                                MessageUtil.getWorldName(spawn.getLocation().worldName()),
                                spawn.getLocation().x(),
                                spawn.getLocation().y(),
                                spawn.getLocation().z()), DefinedTextColor.GRAY));
            }
        }

        if (!villages.isEmpty()) {
            message = message.appendNewline()
                    .append(Component.text("--- 公認村 (" + villages.size() + ") ---", DefinedTextColor.GOLD));
            for (Village village : villages) {
                message = message.appendNewline()
                        .append(Component.text("  unknown-network:villages/" + village.getName(), DefinedTextColor.AQUA))
                        .append(Component.text(" - ", DefinedTextColor.GRAY))
                        .append(village.getDisplayName().hoverEvent(HoverEvent.showText(village.getDescription())))
                        .append(Component.text(" - ", DefinedTextColor.GRAY))
                        .append(Component.text(String.format("%s, %.1f, %.1f, %.1f",
                                MessageUtil.getWorldName(village.getLocation().worldName()),
                                village.getLocation().x(),
                                village.getLocation().y(),
                                village.getLocation().z()), DefinedTextColor.GRAY));
            }
        }

        NewMessageUtil.sendMessage(ctx.getSource(), message, false);
        return 1;
    }

    private static int execAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier identifier = IdentifierArgument.getId(ctx, "識別子");
        Spawn existSpawn = Spawns.getSpawn(identifier);
        if (existSpawn != null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.empty()
                    .append(Component.text("識別子 "))
                    .append(Component.text(identifier.toString()))
                    .append(Component.text(" は既にスポーン地点 "))
                    .append(existSpawn.getDisplayName())
                    .append(Component.text(" で使用されています")));
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "名前");
        Component displayName = NewMessageUtil.convertMinecraft2Adventure(ComponentArgument.getRawComponent(ctx, "表示名"));
        Component description = NewMessageUtil.convertMinecraft2Adventure(ComponentArgument.getRawComponent(ctx, "説明"));
        Vec3 coordinates = Vec3Argument.getVec3(ctx, "座標");
        Vec2 rotation = BrigadierUtil.isArgumentKeyExists(ctx, "向き") ? RotationArgument.getRotation(ctx, "向き").getRotation(ctx.getSource()) : ctx.getSource().getRotation();
        Location location = MinecraftAdapter.location(ctx.getSource().getLevel(), coordinates, rotation);

        net.minecraft.world.item.ItemStack nmsIcon = ItemArgument.getItem(ctx, "アイコン").createItemStack(1);
        ItemStack icon = MinecraftAdapter.ItemStack.itemStack(nmsIcon);

        Spawns.addSpawn(identifier, name, displayName, description, icon, location);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("スポーン地点 "))
                .append(displayName)
                .append(Component.text(" を作成しました"))
                .append(icon != null ? Component.text(" (アイコン: " + icon.getType().name() + ")") : Component.empty()));
        return 1;
    }

    private static int execRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier identifier = IdentifierArgument.getId(ctx, "識別子");
        Spawn spawn = Spawns.getSpawn(identifier);
        if (spawn == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.empty()
                    .append(Component.text("識別子 "))
                    .append(Component.text(identifier.toString()))
                    .append(Component.text(" のスポーン地点は見つかりませんでした")));
            return 0;
        }

        Spawns.removeSpawn(identifier);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("スポーン地点 "))
                .append(spawn.getDisplayName())
                .append(Component.text(" を削除しました")));
        return 1;
    }
}