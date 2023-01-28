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

package net.unknown.survival.chat;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.enums.Permissions;
import net.unknown.core.prefix.PlayerPrefixes;
import net.unknown.core.prefix.Prefix;
import net.unknown.core.util.YukiKanaConverter;
import net.unknown.survival.UnknownNetworkSurvival;
import net.unknown.survival.chat.channels.ChannelType;
import net.unknown.survival.chat.channels.ChatChannel;
import net.unknown.survival.chat.channels.GlobalChannel;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.dependency.LuckPerms;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.regex.Pattern;

public class ChatManager implements Listener {
    private static final Map<UUID, ChatChannel> DEFAULT_CHANNELS = new HashMap<>();

    public static boolean setChannel(UUID uniqueId, ChatChannel channel) {
        if (ChatManager.getCurrentChannel(uniqueId).getType() == channel.getType()) return false;

        if (channel.getType() == ChannelType.HEADS_UP && !UnknownNetworkSurvival.isHolographicDisplaysEnabled()) {
            return false;
        }

        if (DEFAULT_CHANNELS.containsKey(uniqueId)) {
            DEFAULT_CHANNELS.get(uniqueId).onChannelSwitch(channel);
        }

        if (channel.getType() != ChannelType.GLOBAL) DEFAULT_CHANNELS.put(uniqueId, channel);
        else DEFAULT_CHANNELS.remove(uniqueId);
        return true;
    }

    public static ChatChannel getCurrentChannel(UUID uniqueId) {
        return DEFAULT_CHANNELS.getOrDefault(uniqueId, GlobalChannel.getInstance());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Component base = Component.empty();

            Prefix activePrefix = PlayerPrefixes.getActivePrefix(source.getUniqueId());
            Component prefixWrapper = Component.empty();
            prefixWrapper = activePrefix != null ? prefixWrapper.append(activePrefix.getPrefix()).append(Component.space()) : prefixWrapper;

            Component suffix = Component.empty();
            if (UnknownNetworkSurvival.isLuckPermsEnabled()) {
                suffix = LuckPerms.getSuffixAsComponent(source.getUniqueId());
            }
            return base.append(prefixWrapper).append(sourceDisplayName).append(suffix).append(Component.text(": ")).append(message);
        });

        // ColorCode Support
        if (event.getPlayer().hasPermission(Permissions.FEATURE_USE_COLOR_CODE.getPermissionNode())) {
            event.message(LegacyComponentSerializer.legacyAmpersand().deserialize(PlainTextComponentSerializer.plainText().serialize(event.message())));
        }

        // URL to Clickable-Link
        event.message(event.message().replaceText((b) -> {
            b.match(Pattern.compile("https?://\\S+")).replacement((r, b2) -> Component.text(b2.content(), Style.style(DefinedTextColor.AQUA, TextDecoration.UNDERLINED)).clickEvent(ClickEvent.openUrl(b2.content())));
        }));

        if (PlayerData.of(event.getPlayer()).getChatData().isUseKanaConvert()) {
            ChatRenderer baseRenderer = event.renderer();
            event.renderer((source, displayName, message, viewer) -> {
                String msgStr = PlainTextComponentSerializer.plainText().serialize(message);
                if (YukiKanaConverter.isNeedToJapanize(msgStr)) {
                    String kanaMsgStr = YukiKanaConverter.conv(msgStr);
                    Component msg = PlainTextComponentSerializer.plainText().deserialize(kanaMsgStr);
                    return baseRenderer.render(source, displayName, Component.empty()
                            .append(msg)
                            .append(Component.text(" (" + msgStr + ")",
                                    Style.style(DefinedTextColor.GRAY, TextDecoration.ITALIC.withState(true)))), viewer);
                }
                return baseRenderer.render(source, displayName, message, viewer);
            });
        }

        if (PlainTextComponentSerializer.plainText().serialize(event.message()).startsWith(PlayerData.of(event.getPlayer()).getChatData().getForceGlobalChatPrefix())) {
            event.message(event.message().replaceText((b) -> {
                b.match(PlayerData.of(event.getPlayer()).getChatData().getForceGlobalChatPrefix()).once().replacement(Component.empty());
            }));
            GlobalChannel.getInstance().processChat(event);
        } else {
            getCurrentChannel(event.getPlayer().getUniqueId()).processChat(event);
        }
    }
}
