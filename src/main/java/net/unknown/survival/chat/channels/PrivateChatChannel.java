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
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.chat.CustomChatTypes;
import net.unknown.core.events.PrivateMessageEvent;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PrivateChatChannel extends ChatChannel {
    private UUID target;

    public PrivateChatChannel(UUID target) {
        super("個人(" + Bukkit.getOfflinePlayer(target).getName() + ")", ChannelType.PRIVATE);
        this.target = target;
    }

    public UUID getTarget() {
        return target;
    }

    public void setTarget(UUID target) {
        this.target = target;
    }

    @Override
    public void processChat(AsyncChatEvent event) {
        //event.setCancelled(true);
        OfflinePlayer player = Bukkit.getOfflinePlayer(this.target);
        if (!player.isOnline()) {
            NewMessageUtil.sendErrorMessage(event.getPlayer(), player.getName() + " はオフラインです。");
            event.setCancelled(true);
            return;
        }

        ServerPlayer sender = ((CraftPlayer) event.getPlayer()).getHandle();
        ServerPlayer receiver = ((CraftPlayer) player).getHandle();
        PrivateMessageEvent pEvent = new PrivateMessageEvent(sender.getBukkitEntity(), Collections.singleton(receiver.getBukkitEntity()), event.signedMessage());
        try {
            Bukkit.getScheduler().callSyncMethod(UnknownNetworkCore.getInstance(), pEvent::callEvent).get(1, TimeUnit.SECONDS);
            if (pEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            NewMessageUtil.sendErrorMessage(event.getPlayer(), "チャットメッセージの処理中にエラーが発生しました: " + e.getLocalizedMessage());
            event.setCancelled(true);
            return;
        }

        //Component message = MessageUtil.convertAdventure2NMS(pEvent.message());
        if (pEvent.isDirty()) event.message(pEvent.message());

        event.viewers().removeIf(viewer -> !viewer.equals(receiver.getBukkitEntity()));

        PlayerChatMessage originalMessage = MinecraftAdapter.Adventure.playerChatMessage(event.signedMessage());
        boolean isNotModifiedMessage = Objects.equals(originalMessage.requireResult().message().component(), event.message());
        OutgoingChatMessage message = OutgoingChatMessage.create(isNotModifiedMessage ? originalMessage : originalMessage.withUnsignedContent(NewMessageUtil.convertAdventure2Minecraft(event.message())));

        event.viewers().forEach(viewer -> {
            if (viewer instanceof Player bukkitViewerPlayer) {
                ServerPlayer minecraftViewerPlayer = MinecraftAdapter.player(bukkitViewerPlayer);
                if (minecraftViewerPlayer != null) {
                    sender.sendChatMessage(message, sender.shouldFilterMessageTo(minecraftViewerPlayer), ChatType.bind(CustomChatTypes.PRIVATE_MESSAGE_OUTGOING, minecraftViewerPlayer));
                    minecraftViewerPlayer.sendChatMessage(message, minecraftViewerPlayer.shouldFilterMessageTo(sender), ChatType.bind(CustomChatTypes.PRIVATE_MESSAGE_INCOMING, sender));
                }
            }
        });
        event.setCancelled(true);

        PlayerData.of(receiver.getUUID()).getChatData().setPrivateMessageReplyTarget(sender.getUUID());
    }
}
