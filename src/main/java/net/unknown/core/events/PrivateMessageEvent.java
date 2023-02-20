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

package net.unknown.core.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.protocol.packet.PlayerChat;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.craftbukkit.v1_19_R2.command.VanillaCommandWrapper;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PrivateMessageEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final CommandSourceStack source;
    private final Set<ServerPlayer> receivers;
    private boolean cancelled = false;
    private PlayerChatMessage message;

    public PrivateMessageEvent(Entity sender, Collection<Player> receivers, Component message) {
        this.source = VanillaCommandWrapper.getListener(sender);
        this.receivers = receivers.stream().map(player -> (CraftPlayer) player).map(CraftPlayer::getHandle).collect(Collectors.toSet());
        this.message = new PlayerChatMessage(
                SignedMessageLink.unsigned(this.source.getEntity() == null ? Util.NIL_UUID : this.source.getEntity().getUUID()),
                null,
                SignedMessageBody.unsigned(PlainTextComponentSerializer.plainText().serialize(message)),
                NewMessageUtil.convertAdventure2Minecraft(message),
                FilterMask.PASS_THROUGH);
    }

    public PrivateMessageEvent(CommandSourceStack source, Collection<ServerPlayer> receivers, PlayerChatMessage message) {
        this.source = source;
        this.receivers = new HashSet<>(receivers);
        this.message = message;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    public CommandSourceStack getSource() {
        return this.source;
    }

    public Set<Player> getReceivers() {
        return this.receivers.stream().map(ServerPlayer::getBukkitEntity).collect(Collectors.toSet());
    }

    public Component message() {
        return NewMessageUtil.convertMinecraft2Adventure(this.message.unsignedContent());
    }

    public void message(Component message) {
        this.message = new PlayerChatMessage(
                SignedMessageLink.unsigned(this.source.getEntity() == null ? Util.NIL_UUID : this.source.getEntity().getUUID()),
                null,
                SignedMessageBody.unsigned(PlainTextComponentSerializer.plainText().serialize(message)),
                NewMessageUtil.convertAdventure2Minecraft(message),
                FilterMask.PASS_THROUGH);
    }

    public void message(PlayerChatMessage message) {
        this.message = message;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
