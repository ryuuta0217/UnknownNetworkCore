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

package net.unknown.core.feature.admin.spy;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.enums.Permissions;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class Spy implements Listener {
    private static final Spy INSTANCE = new Spy();
    private static final Logger LOGGER = Logger.getLogger("UNC/Spy");
    private static final Set<SpyModule> MODULES = new HashSet<>();
    private static boolean FREEZE = false;

    static {
        ListenerManager.registerListener(INSTANCE);
    }

    private Spy() {}

    @EventHandler
    public void onTickStart(ServerTickStartEvent event) {
        MODULES.forEach(module -> module.onTickStart(event));
    }

    @EventHandler
    public void onTickEnd(ServerTickEndEvent event) {
        MODULES.forEach(module -> module.onTickEnd(event));
    }

    public static void registerModule(SpyModule module) {
        if (FREEZE) throw new UnsupportedOperationException("Spy modules registry is already frozen!");
        module.onRegistering();
        MODULES.add(module);
        LOGGER.info("Module " + module.getIdentifier().asString() + " registered!");
    }

    @Deprecated
    public static void unregisterModule(SpyModule module) {
        if (FREEZE) throw new UnsupportedOperationException("Spy modules registry is already frozen!");
        module.onUnRegistering();
        MODULES.remove(module);
    }

    public static void unregisterModule(NamespacedKey moduleIdentifier) {
        if (FREEZE) throw new UnsupportedOperationException("Spy modules registry is already frozen!");
        MODULES.removeIf(module -> {
            if (module.getIdentifier().equals(moduleIdentifier)) {
                module.onUnRegistering();
                return true;
            }
            return false;
        });
    }

    public static void unregisterAll() {
        if (FREEZE) throw new UnsupportedOperationException("Spy modules registry is already frozen!");
        MODULES.clear();
    }

    public static void freeze() {
        FREEZE = true;
        LOGGER.info("Spy modules registry is frozen!");
    }

    public static void unfreeze() {
        FREEZE = false;
    }

    public static Set<Audience> getSpyMessageReceivers(Predicate<Player> receiverRemoveIf, boolean logConsole) {
        Set<Audience> receivers = new HashSet<>();
        Bukkit.getOnlinePlayers()
                .stream()
                .filter(player -> player.isOp() || player.hasPermission(Permissions.FEATURE_SPY.getPermissionNode()))
                .filter(player -> !receiverRemoveIf.test(player))
                .forEach(receivers::add);
        if (logConsole) receivers.add(Bukkit.getConsoleSender());
        return receivers;
    }

    public static void broadcastSpyMessage(SpyModule source, Component message, Predicate<Player> receiverRemoveIf, boolean logConsole, @Nullable UUID sender) {
        Spy.getSpyMessageReceivers(receiverRemoveIf, logConsole).forEach(audience -> {
            if (audience instanceof Player audiencePlayer) {
                Component spyMessage = Component.empty().color(DefinedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, true)
                        .append(source.getDisplayName())
                        .append(Component.text(">"))
                        .appendSpace()
                        .append(message);
                audience.sendMessage(spyMessage);
                /*if (sender == null) {
                    // audience.sendMessage(spyMessage);
                } else {
                    PlayerChatMessage chatMessage = PlayerChatMessage.unsigned(sender, PlainTextComponentSerializer.plainText().serialize(spyMessage))
                            .withUnsignedContent(NewMessageUtil.convertAdventure2Minecraft(spyMessage));

                    ServerPlayer audienceMinecraftPlayer = MinecraftAdapter.player(audiencePlayer);
                    if (audienceMinecraftPlayer != null) {
                        audienceMinecraftPlayer.sendChatMessage(chatMessage, false, ChatType);
                    }
                }*/
            } else {
                audience.sendMessage(Component.empty()
                        .append(Component.text("[Spy]"))
                        .appendSpace()
                        .append(Component.empty()
                                .append(Component.text("["))
                                .append(source.getDisplayName())
                                .append(Component.text("]")))
                        .appendSpace()
                        .append(message));
            }
        });
    }
}
