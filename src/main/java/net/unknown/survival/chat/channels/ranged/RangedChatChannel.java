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

package net.unknown.survival.chat.channels.ranged;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.util.MessageUtil;
import net.unknown.survival.chat.channels.ChannelType;
import net.unknown.survival.chat.channels.ChatChannel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class RangedChatChannel extends ChatChannel {
    private double range;

    protected RangedChatChannel(String channelName, ChannelType type, double range) {
        super(channelName, type);
        this.range = range;
    }

    public double getRange() {
        return this.range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    @Override
    public void processChat(AsyncChatEvent event) {
        try {
            if (this.range > 0) {
                processRangedChat(event, Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> {
                    List<Entity> entities = new ArrayList<>();
                    entities.add(event.getPlayer());
                    entities.addAll(event.getPlayer().getNearbyEntities(this.range, this.range, this.range));
                    return entities;
                }).get().stream().filter(e -> e instanceof Player).map(e -> (Player) e).toList());
            } else {
                processRangedChat(event, Bukkit.getOnlinePlayers());
            }
        } catch (InterruptedException | ExecutionException e) {
            onException(event, e);
        }
    }

    protected void onException(AsyncChatEvent event, Exception e) {
        e.printStackTrace();
        MessageUtil.sendMessage(event.getPlayer(), "メッセージの配信中にエラーが発生しました: " + e.getLocalizedMessage());
        event.setCancelled(false);
    }

    public abstract void processRangedChat(AsyncChatEvent event, Collection<? extends Player> receivers);
}
