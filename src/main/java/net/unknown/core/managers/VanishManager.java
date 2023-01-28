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

package net.unknown.core.managers;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.enums.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VanishManager {
    private static final Set<UUID> VANISHED_PLAYERS = new HashSet<>();

    public static boolean isVanished(ServerPlayer player) {
        return isVanished(player.getUUID());
    }

    public static boolean isVanished(Player player) {
        return isVanished(player.getUniqueId());
    }

    public static boolean isVanished(UUID uniqueId) {
        return VANISHED_PLAYERS.contains(uniqueId);
    }

    public static boolean vanish(ServerPlayer player, boolean silent) {
        return false;
    }

    private static void removeFromTabList(ServerPlayer removeTarget) {
        MinecraftServer.getServer().getPlayerList().getPlayers().forEach(sendTarget -> removeFromTabList(removeTarget, sendTarget));
    }

    private static void removeFromTabList(ServerPlayer removeTarget, ServerPlayer sendTarget) {
        if (sendTarget.getBukkitEntity().hasPermission(Permissions.FEATURE_SEE_VANISHED_PLAYERS.getPermissionNode()))
            return;
        if (sendTarget.getUUID().equals(removeTarget.getUUID())) return;
        sendTarget.connection.send(new ClientboundPlayerInfoRemovePacket(Stream.of(removeTarget).map(ServerPlayer::getUUID).collect(Collectors.toList())));
    }

    private static void addToTabList(ServerPlayer addTarget) {
        MinecraftServer.getServer().getPlayerList().getPlayers().forEach(sendTarget -> addToTabList(addTarget, sendTarget));
    }

    private static void addToTabList(ServerPlayer addTarget, ServerPlayer sendTarget) {
        if (sendTarget.getUUID().equals(addTarget.getUUID())) return;
        sendTarget.connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, addTarget));
    }

    private static void setHidden(ServerPlayer hideTarget) {
        Bukkit.getOnlinePlayers().forEach(target -> setHidden(hideTarget, target));
    }

    private static void setHidden(ServerPlayer hideTarget, Player target) {
        if (target.hasPermission(Permissions.FEATURE_SEE_VANISHED_PLAYERS.getPermissionNode())) return;
        target.hidePlayer(UnknownNetworkCore.getInstance(), hideTarget.getBukkitEntity());
    }

    private static void setShowing(ServerPlayer showTarget) {
        Bukkit.getOnlinePlayers().forEach(target -> setShowing(showTarget, target));
    }

    private static void setShowing(ServerPlayer showTarget, Player target) {
        target.showPlayer(UnknownNetworkCore.getInstance(), showTarget.getBukkitEntity());
    }
}
