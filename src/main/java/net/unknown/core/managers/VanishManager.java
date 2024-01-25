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

import net.kyori.adventure.text.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.enums.Permissions;
import net.unknown.core.packet.PacketManager;
import net.unknown.core.packet.event.PacketSendingEvent;
import net.unknown.core.packet.listener.OutgoingPacketListener;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VanishManager extends OutgoingPacketListener<ClientboundPlayerInfoUpdatePacket> implements Listener {
    private static final VanishManager INSTANCE = new VanishManager();
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

    public static boolean vanish(Player player, boolean silent) {
        return vanish(MinecraftAdapter.player(player), silent);
    }

    public static boolean vanish(ServerPlayer player, boolean silent) {
        if (isVanished(player)) return false;
        VANISHED_PLAYERS.add(player.getUUID());
        removeFromTabList(player);
        setHidden(player);
        if (!silent) {
            Bukkit.broadcast(Component.translatable("multiplayer.player.left", player.adventure$displayName).color(DefinedTextColor.YELLOW));
        }
        return true;
    }

    public static boolean unvanish(Player player, boolean silent) {
        return unvanish(MinecraftAdapter.player(player), silent);
    }

    public static boolean unvanish(ServerPlayer player, boolean silent) {
        if (!isVanished(player)) return false;
        VANISHED_PLAYERS.remove(player.getUUID());
        addToTabList(player);
        setShowing(player);
        if (!silent) {
            Bukkit.broadcast(Component.translatable("multiplayer.player.joined", player.adventure$displayName).color(DefinedTextColor.YELLOW));
        }
        return true;
    }

    private static void removeFromTabList(Player removeTarget) {
        removeFromTabList(MinecraftAdapter.player(removeTarget));
    }

    private static void removeFromTabList(ServerPlayer removeTarget) {
        MinecraftServer.getServer().getPlayerList().getPlayers().forEach(sendTarget -> removeFromTabList(removeTarget, sendTarget));
    }

    private static void removeFromTabList(ServerPlayer removeTarget, Player sendTarget) {
        removeFromTabList(removeTarget, MinecraftAdapter.player(sendTarget));
    }

    private static void removeFromTabList(Player removeTarget, ServerPlayer sendTarget) {
        removeFromTabList(MinecraftAdapter.player(removeTarget), sendTarget);
    }

    private static void removeFromTabList(Player removeTarget, Player sendTarget) {
        removeFromTabList(MinecraftAdapter.player(removeTarget), MinecraftAdapter.player(sendTarget));
    }

    private static void removeFromTabList(ServerPlayer removeTarget, ServerPlayer sendTarget) {
        if (sendTarget.getBukkitEntity().hasPermission(Permissions.FEATURE_SEE_VANISHED_PLAYERS.getPermissionNode())) {
            removeTarget.listName = removeTarget.getName().copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            sendTarget.connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, removeTarget));
            removeTarget.listName = null;
            return;
        }
        if (sendTarget.getUUID().equals(removeTarget.getUUID())) return;
        sendTarget.connection.send(new ClientboundPlayerInfoRemovePacket(Stream.of(removeTarget).map(ServerPlayer::getUUID).collect(Collectors.toList())));
    }

    private static void addToTabList(Player addTarget) {
        addToTabList(MinecraftAdapter.player(addTarget));
    }

    private static void addToTabList(ServerPlayer addTarget) {
        MinecraftServer.getServer().getPlayerList().getPlayers().forEach(sendTarget -> addToTabList(addTarget, sendTarget));
    }

    private static void addToTabList(ServerPlayer addTarget, Player sendTarget) {
        addToTabList(addTarget, MinecraftAdapter.player(sendTarget));
    }

    private static void addToTabList(Player addTarget, ServerPlayer sendTarget) {
        addToTabList(MinecraftAdapter.player(addTarget), sendTarget);
    }

    private static void addToTabList(Player addTarget, Player sendTarget) {
        addToTabList(MinecraftAdapter.player(addTarget), MinecraftAdapter.player(sendTarget));
    }

    private static void addToTabList(ServerPlayer addTarget, ServerPlayer sendTarget) {
        if (sendTarget.getBukkitEntity().hasPermission(Permissions.FEATURE_SEE_VANISHED_PLAYERS.getPermissionNode())) {
            addTarget.listName = addTarget.getName();
            sendTarget.connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, addTarget));
            addTarget.listName = null;
            return;
        }
        if (sendTarget.getUUID().equals(addTarget.getUUID())) return;
        sendTarget.connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, addTarget));
    }

    private static void setHidden(Player hideTarget) {
        setHidden(MinecraftAdapter.player(hideTarget));
    }

    private static void setHidden(ServerPlayer hideTarget) {
        Bukkit.getOnlinePlayers().forEach(target -> setHidden(hideTarget, target));
    }

    private static void setHidden(ServerPlayer hideTarget, Player target) {
        setHidden(hideTarget, MinecraftAdapter.player(target));
    }

    private static void setHidden(Player hideTarget, ServerPlayer target) {
        setHidden(MinecraftAdapter.player(hideTarget), target);
    }

    private static void setHidden(Player hideTarget, Player target) {
        setHidden(MinecraftAdapter.player(hideTarget), MinecraftAdapter.player(target));
    }

    private static void setHidden(ServerPlayer hideTarget, ServerPlayer target) {
        if (target.getBukkitEntity().hasPermission(Permissions.FEATURE_SEE_VANISHED_PLAYERS.getPermissionNode())) return;
        target.getBukkitEntity().hidePlayer(UnknownNetworkCorePlugin.getInstance(), hideTarget.getBukkitEntity());
    }

    private static void setShowing(Player showTarget) {
        setShowing(MinecraftAdapter.player(showTarget));
    }

    private static void setShowing(ServerPlayer showTarget) {
        Bukkit.getOnlinePlayers().forEach(target -> setShowing(showTarget, target));
    }

    private static void setShowing(ServerPlayer showTarget, Player target) {
        setShowing(showTarget, MinecraftAdapter.player(target));
    }

    private static void setShowing(Player showTarget, ServerPlayer target) {
        setShowing(MinecraftAdapter.player(showTarget), target);
    }

    private static void setShowing(Player showTarget, Player target) {
        setShowing(MinecraftAdapter.player(showTarget), MinecraftAdapter.player(target));
    }

    private static void setShowing(ServerPlayer showTarget, ServerPlayer target) {
        target.getBukkitEntity().showPlayer(UnknownNetworkCorePlugin.getInstance(), showTarget.getBukkitEntity());
    }

    private VanishManager() {
        ListenerManager.registerListener(this);
        PacketManager.getInstance().registerOutgoingS2CListener(ClientboundPlayerInfoUpdatePacket.class, this);
    }

    @Override
    public void onSendingPacket(PacketSendingEvent<ClientboundPlayerInfoUpdatePacket> event) {
        if (event.getPlayer().hasPermission(Permissions.FEATURE_SEE_VANISHED_PLAYERS.getPermissionNode())) return;

        if (event.getPacket().actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
            event.getPacket().entries().removeIf(player -> isVanished(player.profileId()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (isVanished(event.getPlayer())) { // If vanished player logged in, hide from all players.
            event.joinMessage(null);
            //RunnableManager.runDelayed(() -> vanish(event.getPlayer(), true), 1);
        } else if (!event.getPlayer().hasPermission(Permissions.FEATURE_SEE_VANISHED_PLAYERS.getPermissionNode())) {
            VANISHED_PLAYERS.forEach(uuid -> {
                Player vanished = Bukkit.getPlayer(uuid);
                if (vanished != null) {
                    removeFromTabList(vanished, event.getPlayer());
                    setHidden(vanished, event.getPlayer());
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (isVanished(event.getPlayer())) {
            event.quitMessage(null);
        }
    }
}
