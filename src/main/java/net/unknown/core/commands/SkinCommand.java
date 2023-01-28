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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.BiomeManager;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.enums.Permissions;
import net.unknown.core.managers.SkinManager;
import net.unknown.core.util.MessageUtil;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SkinCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("skin");
        builder.requires(Permissions.COMMAND_SKIN::checkAndIsPlayer);
        builder.then(Commands.argument("skinPlayerName", StringArgumentType.word())
                .suggests(Suggestions.ALL_PLAYER_SUGGEST)
                .executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer)) {
                        MessageUtil.sendErrorMessage(ctx.getSource(), "プレイヤーが実行する必要があります。");
                        return 1;
                    }

                    String skinPlayerName = StringArgumentType.getString(ctx, "skinPlayerName");
                    UUID skinPlayerUniqueId = Bukkit.getPlayerUniqueId(skinPlayerName);
                    if (skinPlayerUniqueId == null) {
                        MessageUtil.sendErrorMessage(ctx.getSource(), "プレイヤー " + skinPlayerName + " は見つかりませんでした");
                        return -1;
                    }

                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    SkinManager.Skin skinData = SkinManager.getSkin(skinPlayerUniqueId);
                    if (skinData == null) {
                        MessageUtil.sendErrorMessage(ctx.getSource(), "データの取得中にエラーが発生しました");
                        return -2;
                    }

                    SkinManager.setSkin(player.getBukkitEntity(), skinData);

                    /*sendSelfUpdatePackets(player);
                    sendOtherUpdatePackets(player);*/

                    MessageUtil.sendMessage(ctx.getSource(), "スキンを変更しました");
                    return 0;
                }));
        dispatcher.register(builder);
    }

    public static void sendSelfUpdatePackets(ServerPlayer player) {
        ClientboundPlayerInfoRemovePacket toRemove = new ClientboundPlayerInfoRemovePacket(Stream.of(player).map(ServerPlayer::getUUID).collect(Collectors.toList()));
        ClientboundPlayerInfoUpdatePacket toAdd = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, player);
        //this.connection.send(new ClientboundRespawnPacket(
        // worldserver.dimensionTypeId(),
        // worldserver.dimension(),
        // BiomeManager.obfuscateSeed(worldserver.getSeed()),
        // this.gameMode.getGameModeForPlayer(),
        // this.gameMode.getPreviousGameModeForPlayer(),
        // worldserver.isDebug(), worldserver.isFlat(), true, this.getLastDeathLocation()));
        ClientboundRespawnPacket respawn = new ClientboundRespawnPacket(
                player.getLevel().dimensionTypeId(),
                player.getLevel().dimension(),
                BiomeManager.obfuscateSeed(player.getLevel().getSeed()),
                player.gameMode.getGameModeForPlayer(),
                player.gameMode.getPreviousGameModeForPlayer(),
                player.getLevel().isDebug(),
                player.getLevel().isFlat(),
                (byte) 3,
                player.getLastDeathLocation());
        ClientboundPlayerPositionPacket teleport = new ClientboundPlayerPositionPacket(
                player.position().x(),
                player.position().y(),
                player.position().z(),
                player.getRotationVector().y,
                player.getRotationVector().x,
                new HashSet<>(),
                -1,
                false);

        player.connection.send(toRemove);
        player.connection.send(toAdd);
        player.connection.send(respawn);
        player.connection.send(teleport);
    }

    public static void sendOtherUpdatePackets(ServerPlayer updateTarget) {
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (p.getUniqueId().equals(updateTarget.getUUID())) return;
            p.hidePlayer(UnknownNetworkCore.getInstance(), updateTarget.getBukkitEntity());
            p.showPlayer(UnknownNetworkCore.getInstance(), updateTarget.getBukkitEntity());
        });
    }
}
