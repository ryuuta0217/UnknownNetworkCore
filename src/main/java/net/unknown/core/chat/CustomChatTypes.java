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

package net.unknown.core.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ChatTypeDecoration;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.unknown.core.util.RegistryUtil;
import org.bukkit.NamespacedKey;

import java.util.List;

public class CustomChatTypes {
    public static final ResourceKey<ChatType> PRIVATE_MESSAGE_INCOMING = ResourceKey.create(Registries.CHAT_TYPE, new ResourceLocation("private_message_incoming"));
    public static final ChatType VALUE_PRIVATE_MESSAGE_INCOMING = new ChatType(
            new ChatTypeDecoration( // for chat view
                    "§b[PM]§r [%s] %s",
                    List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT),
                    Style.EMPTY),
            new ChatTypeDecoration( // for narration
                    "%s があなたに %s と言いました",
                    List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT),
                    Style.EMPTY));
    public static final net.kyori.adventure.chat.ChatType PRIVATE_MESSAGE_INCOMING_ADVENTURE = net.kyori.adventure.chat.ChatType.chatType(NamespacedKey.fromString(PRIVATE_MESSAGE_INCOMING.location().toString()));

    public static final ResourceKey<ChatType> PRIVATE_MESSAGE_OUTGOING = ResourceKey.create(Registries.CHAT_TYPE, new ResourceLocation("private_message_outgoing"));
    public static final ChatType VALUE_PRIVATE_MESSAGE_OUTGOING = new ChatType(
            new ChatTypeDecoration( // for chat view
                    "§b[PM]§r [→ %s] %s",
                    List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT),
                    Style.EMPTY),
            new ChatTypeDecoration( // for narration
                    "あなたが %s に %s と言いました",
                    List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT),
                    Style.EMPTY));
    public static final net.kyori.adventure.chat.ChatType PRIVATE_MESSAGE_OUTGOING_ADVENTURE = net.kyori.adventure.chat.ChatType.chatType(NamespacedKey.fromString(PRIVATE_MESSAGE_OUTGOING.location().toString()));

    public static final ResourceKey<ChatType> PRIVATE_MESSAGE = ResourceKey.create(Registries.CHAT_TYPE, new ResourceLocation("private_message"));
    public static final ChatType VALUE_PRIVATE_MESSAGE = new ChatType(
            new ChatTypeDecoration( // for chat view
                    "§b[PM]§r [%s -> %s] %s",
                    List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.TARGET, ChatTypeDecoration.Parameter.CONTENT),
                    Style.EMPTY),
            new ChatTypeDecoration( // for narration
                    "%s が %s に %s と言いました",
                    List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.TARGET, ChatTypeDecoration.Parameter.CONTENT),
                    Style.EMPTY));
    public static final net.kyori.adventure.chat.ChatType PRIVATE_MESSAGE_ADVENTURE = net.kyori.adventure.chat.ChatType.chatType(NamespacedKey.fromString(PRIVATE_MESSAGE.location().toString()));

    // Example: CommandSpy> Yncryption: /gamemode creative
    public static final ResourceKey<ChatType> COMMAND_SPY = ResourceKey.create(Registries.CHAT_TYPE, new ResourceLocation("command_spy"));
    public static final ChatType VALUE_COMMAND_SPY = new ChatType(
            new ChatTypeDecoration( // for chat view
                    "CommandSpy> %s: %s",
                    List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT),
                    Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)),
            new ChatTypeDecoration( // for narration
                    "%s がコマンド %s を実行しました",
                    List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT),
                    Style.EMPTY));
    public static final net.kyori.adventure.chat.ChatType COMMAND_SPY_ADVENTURE = net.kyori.adventure.chat.ChatType.chatType(NamespacedKey.fromString(COMMAND_SPY.location().toString()));

    public static void bootstrap() {
        register(Registries.CHAT_TYPE, PRIVATE_MESSAGE_INCOMING, VALUE_PRIVATE_MESSAGE_INCOMING);
        register(Registries.CHAT_TYPE, PRIVATE_MESSAGE_OUTGOING, VALUE_PRIVATE_MESSAGE_OUTGOING);
        register(Registries.CHAT_TYPE, PRIVATE_MESSAGE, VALUE_PRIVATE_MESSAGE);

        register(Registries.CHAT_TYPE, COMMAND_SPY, VALUE_COMMAND_SPY);
    }

    @SuppressWarnings("unchecked")
    public static ResourceKey<ChatType> register(ResourceKey<Registry<ChatType>> registry, ResourceKey<ChatType> key, ChatType type) {
        Registry<ChatType> chatTypes = MinecraftServer.getServer().registryAccess().registry(Registries.CHAT_TYPE).orElse(null);
        if (chatTypes != null) {
            RegistryUtil.forceRegister(chatTypes, key.location(), type);
            return key;
        }

        return null;
    }
}
