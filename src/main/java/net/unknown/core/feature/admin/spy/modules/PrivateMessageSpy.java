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

package net.unknown.core.feature.admin.spy.modules;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.core.chat.CustomChatTypes;
import net.unknown.core.events.PrivateMessageEvent;
import net.unknown.core.feature.admin.spy.Spy;
import net.unknown.core.feature.admin.spy.SpyModule;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.function.Predicate;

public class PrivateMessageSpy implements SpyModule, Listener {
    public static final NamespacedKey IDENTIFIER = new NamespacedKey("unknown-network", "private_message");
    public static final Component NAME = Component.text("PrivateMessageSpy");

    @Override
    public void onRegistering() {
        ListenerManager.registerListener(this);
    }

    @Override
    public void onUnRegistering() {
        ListenerManager.unregisterListener(this);
    }

    @Override
    public NamespacedKey getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Component getDisplayName() {
        return NAME;
    }

    @EventHandler
    public void onPrivateMessage(PrivateMessageEvent event) {
        ServerPlayer sourcePlayer = event.getSource().getPlayer();
        if (sourcePlayer == null) return;

        Component sourceDisplayName = sourcePlayer.adventure$displayName;
        event.getReceivers().forEach(receiver -> {
            broadcastSpyMessage(event.playerChatMessage(), sourceDisplayName, receiver.displayName(), event.message(), player -> player.getUniqueId().equals(receiver.getUniqueId()) || player.getUniqueId().equals(sourcePlayer.getUUID()));
        });
    }

    private static void broadcastSpyMessage(PlayerChatMessage originalMessage, Component sourceDisplayName, Component targetDisplayName, Component message, Predicate<Player> receiverRemoveIf) {
        Spy.getSpyMessageReceivers(receiverRemoveIf, true).forEach(audience -> {
            if (audience instanceof Player audiencePlayer) {
                ServerPlayer audienceMinecraftPlayer = MinecraftAdapter.player(audiencePlayer);
                if (audienceMinecraftPlayer != null) {
                    audienceMinecraftPlayer.sendChatMessage(OutgoingChatMessage.create(originalMessage), false, CustomChatTypes.VALUE_PRIVATE_MESSAGE.bind(NewMessageUtil.convertAdventure2Minecraft(sourceDisplayName)).withTargetName(NewMessageUtil.convertAdventure2Minecraft(targetDisplayName)));
                }
            } else {
                audience.sendMessage(NewMessageUtil.convertMinecraft2Adventure(CustomChatTypes.VALUE_PRIVATE_MESSAGE.chat().decorate(NewMessageUtil.convertAdventure2Minecraft(LegacyComponentSerializer.legacySection().deserialize(PlainTextComponentSerializer.plainText().serialize(message))), CustomChatTypes.VALUE_PRIVATE_MESSAGE.bind(NewMessageUtil.convertAdventure2Minecraft(sourceDisplayName)).withTargetName(NewMessageUtil.convertAdventure2Minecraft(targetDisplayName)))));
            }
        });
    }
}
