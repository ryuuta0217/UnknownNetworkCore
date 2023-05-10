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

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ChatTypeDecoration;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class CustomChatTypes {
    public static final ResourceKey<ChatType> OUTGOING_CUSTOM_CHANNEL_CHAT = ResourceKey.create(Registries.CHAT_TYPE, new ResourceLocation("custom_chat_channel_outgoing"));
    public static final ResourceKey<ChatType> INCOMING_CUSTOM_CHANNEL_CHAT = ResourceKey.create(Registries.CHAT_TYPE, new ResourceLocation("custom_chat_channel_incoming"));
    public static final ResourceKey<ChatType> FREEDOM = ResourceKey.create(Registries.CHAT_TYPE, new ResourceLocation("freedom"));

    public static void bootstrap() {
        net.unknown.core.chat.CustomChatTypes.register(Registries.CHAT_TYPE, OUTGOING_CUSTOM_CHANNEL_CHAT, new ChatType(
                new ChatTypeDecoration(
                        "%s %s: %s",
                        List.of(ChatTypeDecoration.Parameter.TARGET, ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT),
                        Style.EMPTY),
                new ChatTypeDecoration(
                        "あなたが %s で %s と言いました",
                        List.of(ChatTypeDecoration.Parameter.TARGET, ChatTypeDecoration.Parameter.CONTENT),
                        Style.EMPTY
                )
        ));

        net.unknown.core.chat.CustomChatTypes.register(Registries.CHAT_TYPE, INCOMING_CUSTOM_CHANNEL_CHAT, new ChatType(
                new ChatTypeDecoration(
                        "%s %s: %s",
                        List.of(ChatTypeDecoration.Parameter.TARGET, ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT),
                        Style.EMPTY
                ),
                new ChatTypeDecoration(
                        "%s が %s で %s と言いました",
                        List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.TARGET, ChatTypeDecoration.Parameter.CONTENT),
                        Style.EMPTY
                )
        ));

        net.unknown.core.chat.CustomChatTypes.register(Registries.CHAT_TYPE, FREEDOM, new ChatType(
                new ChatTypeDecoration("%s", List.of(ChatTypeDecoration.Parameter.CONTENT), Style.EMPTY),
                new ChatTypeDecoration("%s", List.of(ChatTypeDecoration.Parameter.TARGET), Style.EMPTY)
        ));
    }
}
