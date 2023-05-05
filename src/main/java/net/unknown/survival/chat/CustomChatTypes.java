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
