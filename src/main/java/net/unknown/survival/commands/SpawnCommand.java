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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.PlayerData;
import net.unknown.core.dependency.MultiverseCore;
import net.unknown.survival.enums.Permissions;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Set;

public class SpawnCommand {
    private static final NamespacedKey SPAWN_COMMAND_REGISTRY_KEY = new NamespacedKey("unknown-network", "spawn_command");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("spawn");
        builder.requires(Permissions.COMMAND_SPAWN::check);
        builder.executes(ctx -> {
            Entity executor = ctx.getSource().getEntityOrException();
            ServerLevel currentLevel = (ServerLevel) executor.level();
            ServerLevel whereToSpawn = MinecraftServer.getServer().getLevel(Level.OVERWORLD); // fallback

            switch (getSpawnCommandMode(ctx.getSource())) {
                case TELEPORT_TO_MAIN_WORLD_SPAWN -> whereToSpawn = MinecraftServer.getServer().getLevel(Level.OVERWORLD);
                case TELEPORT_TO_CURRENT_WORLD_SPAWN -> whereToSpawn = currentLevel;
                case TELEPORT_TO_MAIN_WORLD_WHEN_IN_CURRENT_WORLD_SPAWN -> {
                    Location currentPos = ctx.getSource().getBukkitEntity().getLocation();
                    Location currentWorldSpawn = MultiverseCore.getSpawnLocation(ctx.getSource().getLevel());
                    if (currentWorldSpawn.distance(currentPos) <= 3) {
                        whereToSpawn = MinecraftServer.getServer().getLevel(Level.OVERWORLD);
                    } else {
                        whereToSpawn = ctx.getSource().getLevel();
                    }
                }
            }

            Location whereToTeleport = MultiverseCore.getSpawnLocation(whereToSpawn);
            ctx.getSource().getEntityOrException().teleportTo(whereToSpawn, whereToTeleport.getX(), whereToTeleport.getY(), whereToTeleport.getZ(), Set.of(), whereToTeleport.getYaw(), whereToTeleport.getPitch(), PlayerTeleportEvent.TeleportCause.COMMAND);
            NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                    .append(Component.text(MessageUtil.getWorldName(whereToSpawn.getWorld())))
                    .append(Component.text("ワールドのスポーン地点にテレポートしました")));
            return 0;
        });

        LiteralArgumentBuilder<CommandSourceStack> configureModeNode = Commands.literal("mode")
                .executes(ctx -> {
                    SpawnCommandMode mode = getSpawnCommandMode(ctx.getSource());
                    NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                            .append(Component.text("スポーンコマンドの動作モードは "))
                            .append(Component.text(mode.getLiteral()).hoverEvent(HoverEvent.showText(mode.getDescription())))
                            .append(Component.text(" に設定されています。")));
                    return 0;
                });

        for (SpawnCommandMode mode : SpawnCommandMode.values()) {
            configureModeNode.then(Commands.literal(mode.getLiteral())
                    .executes(ctx -> {
                        setSpawnCommandMode(ctx.getSource(), mode);
                        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                                .append(Component.text("スポーンコマンドの動作モードを変更しました: "))
                                .append(mode.getDescription()));
                        return 0;
                    }));
        }

        builder.then(
                Commands.literal("configure")
                        .requires(Permissions.COMMAND_SPAWN::checkAndIsPlayer)
                        .then(configureModeNode)
        );

        dispatcher.register(builder);
    }

    private static SpawnCommandMode getSpawnCommandMode(CommandSourceStack source) throws CommandSyntaxException {
        SpawnCommandMode mode = SpawnCommandMode.TELEPORT_TO_MAIN_WORLD_SPAWN;
        if (source.getEntity() instanceof Player player) { // CommandSourceStack#getEntity を使用することで、/execute as .. で実行された場合に、対象となるプレイヤーのデータを参照する
            PlayerData pd = PlayerData.of(player.getUUID());
            PlayerData.PlayerRegistry pr = pd.getRegistries();
            if (pr.containsKey(SPAWN_COMMAND_REGISTRY_KEY, "mode")) {
                try {
                    mode = SpawnCommandMode.valueOf(pr.get(SPAWN_COMMAND_REGISTRY_KEY, "mode"));
                } catch (Throwable ignored) {
                } // config <mode> で更新されれば治るはずなので握りつぶします。そもそも(SpawnCommandModeが増えたり消えたりしない限りは)到達しないはず...
            }
        }
        return mode;
    }

    private static void setSpawnCommandMode(CommandSourceStack source, SpawnCommandMode mode) throws CommandSyntaxException {
        if (source.getEntity() instanceof Player player) {
            PlayerData pd = PlayerData.of(player.getUUID());
            PlayerData.PlayerRegistry pr = pd.getRegistries();
            pr.put(SPAWN_COMMAND_REGISTRY_KEY, "mode", mode.name());
            RunnableManager.runAsync(pd::save);
        }
    }

    public enum SpawnCommandMode {
        TELEPORT_TO_MAIN_WORLD_SPAWN("teleportToMainWorldSpawn", Component.text("常にメインワールドのスポーン地点にテレポートします")),
        TELEPORT_TO_CURRENT_WORLD_SPAWN("teleportToCurrentWorldSpawn", Component.text("常に実行時点でのワールドのスポーン地点にテレポートします")),
        TELEPORT_TO_MAIN_WORLD_WHEN_IN_CURRENT_WORLD_SPAWN("teleportToMainWorldWhenInCurrentWorldSpawn", Component.text("実行時点でのワールドのスポーン地点周辺にいる場合は、メインワールドにテレポートします。"));

        private final String literal;
        private final Component description;

        SpawnCommandMode(String literal, Component description) {
            this.literal = literal;
            this.description = description;
        }

        public String getLiteral() {
            return literal;
        }

        public Component getDescription() {
            return description;
        }
    }
}
