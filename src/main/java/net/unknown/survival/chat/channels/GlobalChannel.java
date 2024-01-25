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
import net.kyori.adventure.chat.ChatType;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.SignedMessageBody;
import net.unknown.core.managers.VanishManager;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.Bukkit;

public class GlobalChannel extends ChatChannel {
    private static final GlobalChannel INSTANCE = new GlobalChannel();

    private GlobalChannel() {
        super("グローバル", ChannelType.GLOBAL);
    }

    public static GlobalChannel getInstance() {
        return INSTANCE;
    }

    @Override
    public void processChat(AsyncChatEvent event) {
        if (VanishManager.isVanished(event.getPlayer())) {
            NewMessageUtil.sendErrorMessage(event.getPlayer(), Component.text("現在Vanish中です。チャットメッセージを送信するには、Vanishを解除してください。"));
            event.setCancelled(true);
        }
        //event.viewers().removeIf(audience -> audience.equals(Bukkit.getPlayer("Yncryption")));
    }
}
