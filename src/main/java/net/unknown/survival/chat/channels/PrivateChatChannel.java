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

package net.unknown.survival.chat.channels;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.commands.vanilla.MsgCommand;
import net.unknown.core.events.PrivateMessageEvent;
import net.unknown.core.util.MessageUtil;
import net.unknown.survival.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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
        event.setCancelled(true);
        OfflinePlayer player = Bukkit.getOfflinePlayer(this.target);
        if (!player.isOnline()) {
            MessageUtil.sendErrorMessage(event.getPlayer(), player.getName() + " はオフラインです。");
            return;
        }

        ServerPlayer sender = ((CraftPlayer) event.getPlayer()).getHandle();
        ServerPlayer receiver = ((CraftPlayer) player).getHandle();

        PrivateMessageEvent pEvent = new PrivateMessageEvent(sender.getBukkitEntity(), Collections.singleton(receiver.getBukkitEntity()), event.message());
        try {
            Bukkit.getScheduler().callSyncMethod(UnknownNetworkCore.getInstance(), () -> {
                Bukkit.getPluginManager().callEvent(pEvent);
                return null;
            }).get();
        } catch (InterruptedException | ExecutionException ignored) {
        }
        if (pEvent.isCancelled()) return;

        Component message = MessageUtil.convertAdventure2NMS(pEvent.message());

        sender.sendSystemMessage(MsgCommand.senderMessage(receiver.getDisplayName(), message));
        pEvent.getReceivers()
                .stream()
                .map(r -> ((CraftPlayer) r).getHandle())
                .forEach(r -> r.sendSystemMessage(MsgCommand.receiverMessage(sender.getName(), message)));
        PlayerData.of(receiver.getUUID()).setPrivateMessageReplyTarget(sender.getUUID());
    }
}
