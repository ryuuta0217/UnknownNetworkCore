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

package net.unknown.survival.chat.channels;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.RunnableManager;
import net.unknown.survival.chat.ChatManager;
import net.unknown.survival.chat.CustomChannels;
import net.unknown.survival.events.CustomChatChannelEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CustomChannel extends ChatChannel {
    private final Set<UUID> players = new HashSet<>();
    private final UUID owner;
    private final String channelName;
    private final Component displayName;

    public CustomChannel(String channelName, Component displayName, UUID owner) {
        this(channelName, displayName, owner, Collections.emptyList());
    }

    public CustomChannel(String channelName, Component displayName, UUID owner, List<UUID> players) {
        super(channelName, ChannelType.CUSTOM);
        this.channelName = channelName;
        this.displayName = displayName;
        this.owner = owner;
        this.players.add(owner);
        this.players.addAll(players);
    }

    private Component getChannelPrefix(boolean space) {
        Component c = Component.empty()
                .append(Component.text("["))
                .append(this.displayName) // チャンネル名
                .append(Component.text("]"));

        return space ? c.append(Component.text(" ")) : c;
    }

    private Component getRenderedMessage(Player source, Component message) {
        // うんちする // @author Yu_212
        //source.setDisplayName("うんち"); // @author Yu_212
        //source.banPlayerIP("うんちしたので"); // @author Yu_212
        return Component.empty()
                .append(this.getChannelPrefix(false))
                .append(Component.text(" "))
                .append(source.displayName()) // プレイヤー名
                .append(Component.text(": "))
                .append(message);
    }

    @Override
    public void processChat(AsyncChatEvent event) {
        event.setCancelled(true);

        this.sendMessage(event.getPlayer(), this.getRenderedMessage(event.getPlayer(), event.message()));
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    public String getDisplayNameAsPlainText() {
        return PlainTextComponentSerializer.plainText().serialize(this.getDisplayName());
    }

    public UUID getOwner() {
        return this.owner;
    }

    public Set<UUID> getPlayers() {
        return this.players;
    }

    public void addPlayer(UUID uniqueId) {
        if (this.players.contains(uniqueId))
            throw new IllegalArgumentException("プレイヤー " + uniqueId + " はチャンネル " + this.channelName + " に既に参加しています。");
        this.players.add(uniqueId);
        this.sendSystemMessage(Component.text(Bukkit.getOfflinePlayer(uniqueId).getName() + " がチャンネルに参加しました"));
        RunnableManager.runAsync(CustomChannels::save);
    }

    public void removePlayer(UUID uniqueId) {
        if (!this.players.contains(uniqueId))
            throw new IllegalArgumentException("プレイヤー " + uniqueId + " はチャンネル " + this.channelName + " に参加していません");
        this.players.remove(uniqueId);
        if (ChatManager.getCurrentChannel(uniqueId).equals(this)) {
            ChatManager.setChannel(uniqueId, GlobalChannel.getInstance());
        }
        this.sendSystemMessage(Component.text(Bukkit.getOfflinePlayer(uniqueId).getName() + " がチャンネルから退出しました"));
        RunnableManager.runAsync(CustomChannels::save);
    }

    public void sendSystemMessage(Component message) {
        this.sendMessage(null, Component.empty().style(Style.style(DefinedTextColor.GRAY, TextDecoration.ITALIC))
                .append(this.getChannelPrefix(true))
                .append(message));
    }

    public void sendMessage(Player player, Component message) {
        CustomChatChannelEvent event = new CustomChatChannelEvent(!Bukkit.isPrimaryThread(), player, message, this.players.stream()
                .map(Bukkit::getOfflinePlayer)
                .filter(OfflinePlayer::isOnline)
                .map(off -> (Player) off)
                .collect(Collectors.toSet()), this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        Bukkit.getServer().getConsoleSender().sendMessage(player == null ? Identity.nil() : player.identity(), message);
        event.getReceivers().forEach(receiver -> receiver.sendMessage((event.getSender() == null ? Identity.nil() : event.getSender().identity()), event.getRenderedMessage()));
    }
}
