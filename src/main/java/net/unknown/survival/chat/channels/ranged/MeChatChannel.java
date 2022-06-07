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

package net.unknown.survival.chat.channels.ranged;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.text.Component;
import net.minecraft.world.level.block.ComposterBlock;
import net.unknown.survival.chat.channels.ChannelType;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Collection;

public class MeChatChannel extends RangedChatChannel {
    public MeChatChannel(double range) {
        super("自己主張", ChannelType.ME, range);
    }

    @Override
    public void processRangedChat(AsyncChatEvent event, Collection<? extends Player> receivers) {
        receivers.forEach(receiver -> receiver.sendMessage(event.getPlayer(), Component.translatable("chat.type.emote").args(Component.empty().append(event.getPlayer().displayName()).append(Component.text(this.getRange() > 0 ? "[" + this.getRange() + "]" : "")), event.message()), MessageType.CHAT));
    }
}
