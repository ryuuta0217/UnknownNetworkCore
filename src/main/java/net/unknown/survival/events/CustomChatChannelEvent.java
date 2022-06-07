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

package net.unknown.survival.events;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.unknown.survival.chat.channels.CustomChannel;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Set;

public class CustomChatChannelEvent extends Event implements Cancellable {
    public static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;
    private final Player sender;
    private Component renderedMessage;
    private final Set<Player> receivers;
    private final CustomChannel channel;

    public CustomChatChannelEvent(boolean async, Player sender, Component renderedMessage, Set<Player> receivers, CustomChannel channel) {
        super(async);
        this.sender = sender;
        this.renderedMessage = renderedMessage;
        this.receivers = receivers;
        this.channel = channel;
    }

    public Player getSender() {
        return sender;
    }

    public Component getRenderedMessage() {
        return renderedMessage;
    }

    public void setRenderedMessage(Component renderedMessage) {
        this.renderedMessage = renderedMessage;
    }

    public Set<Player> getReceivers() {
        return receivers;
    }

    public boolean isReceiver(Player player) {
        return this.receivers.contains(player);
    }

    public void addReceiver(Player player) {
        this.receivers.add(player);
    }

    public void removeReceiver(Player player) {
        this.receivers.remove(player);
    }

    public CustomChannel getChannel() {
        return channel;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Nonnull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
