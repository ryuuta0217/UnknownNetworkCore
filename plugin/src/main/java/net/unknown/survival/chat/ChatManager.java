/*
 * Copyright (c) 2021 Unknown Network Developers and contributors.
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
 *     loss of use data or profits; or business interpution) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.survival.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.core.enums.Permissions;
import net.unknown.survival.UnknownNetworkSurvival;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatManager implements Listener {
    private static final Map<UUID, ChatMode> CHAT_MODES = new HashMap<>();
    private static final Map<UUID, Double> RANGE = new HashMap<>();

    public static boolean setChatMode(UUID uniqueId, ChatMode chatMode) {
        if(CHAT_MODES.getOrDefault(uniqueId, ChatMode.GLOBAL) == chatMode) return false;

        if(chatMode == ChatMode.HEADS_UP && !UnknownNetworkSurvival.isHolographicDisplaysEnabled()) {
            return false;
        }

        if(chatMode != ChatMode.GLOBAL) CHAT_MODES.put(uniqueId, chatMode);
        else CHAT_MODES.remove(uniqueId);
        return true;
    }

    public static void setRange(UUID uniqueId, double range) {
        if(range > 0) RANGE.put(uniqueId, range);
        else RANGE.remove(uniqueId);
    }

    public static double getRange(UUID uniqueId) {
        return RANGE.getOrDefault(uniqueId, -1D);
    }

    public static ChatMode getChatMode(UUID uniqueId) {
        return CHAT_MODES.getOrDefault(uniqueId, ChatMode.GLOBAL);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if(event.getPlayer().hasPermission(Permissions.FEATURE_USE_COLOR_CODE.getPermissionNode())) {
            event.message(LegacyComponentSerializer.legacyAmpersand().deserialize(PlainTextComponentSerializer.plainText().serialize(event.message())));
        }

        if(getChatMode(event.getPlayer().getUniqueId()) != ChatMode.GLOBAL) {
            getChatMode(event.getPlayer().getUniqueId()).processChat(event);
        }
    }
}
