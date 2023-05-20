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

package net.unknown.survival.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.chat.ChatManager;
import net.unknown.survival.chat.CustomChannels;
import net.unknown.survival.chat.channels.*;
import net.unknown.survival.chat.channels.ranged.MeChatChannel;
import net.unknown.survival.chat.channels.ranged.NearChatChannel;
import net.unknown.survival.chat.channels.ranged.TitleChatChannel;
import net.unknown.survival.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ChannelCommand {
    private static final Map<UUID, Map<UUID, Map<BukkitTask, CustomChannel>>> CHANNEL_INVITES = new HashMap<>();

    private static final Map<UUID, CustomChannel> TO_REMOVE_CONFIRM = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("channel");

        builder.then(Commands.literal("setdefault")
                        .then(Commands.literal("global")
                                .executes(ctx -> setChannel(ctx, ChannelType.GLOBAL)))
                        .then(Commands.literal("heads_up")
                                .executes(ctx -> setChannel(ctx, ChannelType.HEADS_UP)))
                        .then(Commands.literal("title")
                                .executes(ctx -> setChannel(ctx, ChannelType.TITLE))
                                .then(Commands.argument("範囲", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> setChannel(ctx, ChannelType.TITLE))))
                        .then(Commands.literal("me")
                                .executes(ctx -> setChannel(ctx, ChannelType.ME))
                                .then(Commands.argument("範囲", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> setChannel(ctx, ChannelType.ME))))
                        .then(Commands.literal("near")
                                .then(Commands.argument("範囲", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> setChannel(ctx, ChannelType.NEAR))))
                        .then(Commands.literal("private")
                                .then(Commands.argument("対象", EntityArgument.player())
                                        .executes(ctx -> setChannel(ctx, ChannelType.PRIVATE)))
                                .then(Commands.argument("対象", StringArgumentType.string())
                                        .executes(ctx -> setChannel(ctx, ChannelType.PRIVATE))))
                        .then(Commands.argument("チャンネル名", StringArgumentType.word())
                                .suggests(Suggestions.JOINED_CHANNELS_SUGGEST)
                                .executes(ctx -> setChannel(ctx, ChannelType.CUSTOM))))
                .then(Commands.literal("create") // /channel create <channelName> <displayName>
                        .then(Commands.argument("チャンネル名", StringArgumentType.word())
                                .then(Commands.argument("表示名", ComponentArgument.textComponent())
                                        .executes(ChannelCommand::createChannel))))
                .then(Commands.literal("remove")
                        .executes(ChannelCommand::removeChannel) // remove default channel
                        .then(Commands.argument("チャンネル名", StringArgumentType.word())
                                .suggests(Suggestions.OWNED_CHANNELS_SUGGEST)
                                .executes(ChannelCommand::removeChannel)))
                .then(Commands.literal("invite") // /channel invite <target: Single> [channelName]
                        .then(Commands.argument("対象", EntityArgument.player())
                                .executes(ChannelCommand::inviteToChannel) // invite defaulting channel (only for CUSTOM_CHANNEL)
                                .then(Commands.argument("チャンネル名", StringArgumentType.word())
                                        .suggests(Suggestions.JOINED_CHANNELS_SUGGEST)
                                        .executes(ChannelCommand::inviteToChannel)))) // invite specified channel
                .then(Commands.literal("leave")
                        .executes(ChannelCommand::leaveChannel) // leave from default channel (only for CUSTOM_CHANNEL)
                        .then(Commands.argument("チャンネル名", StringArgumentType.word())
                                .suggests(Suggestions.JOINED_CHANNELS_SUGGEST)
                                .executes(ChannelCommand::leaveChannel))) // leave specified channel
                .then(Commands.literal("accept")
                        .executes(ChannelCommand::acceptInvite)
                        .then(Commands.argument("対象", EntityArgument.player())
                                .executes(ChannelCommand::acceptInvite)
                                .then(Commands.argument("チャンネル名", StringArgumentType.word())
                                        .executes(ChannelCommand::acceptInvite))))
                .then(Commands.literal("deny")
                        .executes(ChannelCommand::denyInvite)
                        .then(Commands.argument("対象", EntityArgument.player())
                                .executes(ChannelCommand::denyInvite)
                                .then(Commands.argument("チャンネル名", StringArgumentType.word())
                                        .executes(ChannelCommand::denyInvite))));

        builder.then(Commands.literal("options")
                .then(Commands.literal("global")
                        .then(Commands.literal("forceGlobalChatPrefix")
                                .then(Commands.literal("get")
                                        .executes(ctx -> showGlobalOption(ctx, GlobalOptions.FORCE_GLOBAL_CHAT_PREFIX, false)))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("接頭辞", StringArgumentType.greedyString())
                                                .executes(ctx -> setGlobalOption(ctx, GlobalOptions.FORCE_GLOBAL_CHAT_PREFIX)))))
                        .then(Commands.literal("kanaConvert")
                                .then(Commands.literal("get")
                                        .executes(ctx -> showGlobalOption(ctx, GlobalOptions.KANA_CONVERT, false)))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("有効", BoolArgumentType.bool())
                                                .executes(ctx -> setGlobalOption(ctx, GlobalOptions.KANA_CONVERT)))))));

        dispatcher.register(builder);
    }

    private static int setChannel(CommandContext<CommandSourceStack> ctx, ChannelType newChannelType) throws CommandSyntaxException {
        double range = BrigadierUtil.getArgumentOrDefault(ctx, double.class, "範囲", -1.0D);

        ChatChannel currentChannel = ChatManager.getCurrentChannel(ctx.getSource().getPlayerOrException().getUUID());
        ChatChannel newChannel = null;

        switch (newChannelType) {
            case GLOBAL -> {
                if (!(currentChannel instanceof GlobalChannel)) newChannel = GlobalChannel.getInstance();
            }
            case ME -> {
                if (currentChannel instanceof MeChatChannel m && m.getRange() != range) {
                    m.setRange(range);
                    newChannel = m;
                }

                if (!(currentChannel instanceof MeChatChannel)) {
                    newChannel = new MeChatChannel(range);
                }
            }
            case TITLE -> {
                if (currentChannel instanceof TitleChatChannel t && t.getRange() != range) {
                    t.setRange(range);
                    newChannel = t;
                }

                if (!(currentChannel instanceof TitleChatChannel)) {
                    newChannel = new TitleChatChannel(range);
                }
            }
            case HEADS_UP -> {
                if (!(currentChannel instanceof HeadsUpChatChannel)) {
                    newChannel = new HeadsUpChatChannel(ctx.getSource().getPlayerOrException().getUUID());
                }
            }
            case NEAR -> {
                if (!(currentChannel instanceof NearChatChannel)) {
                    newChannel = new NearChatChannel(range);
                }
            }
            case CUSTOM -> {
                String channelName = BrigadierUtil.getArgumentOrDefault(ctx, String.class, "チャンネル名", null);
                if (CustomChannels.isChannelFound(channelName) && CustomChannels.getChannel(channelName).getPlayers().contains(ctx.getSource().getPlayerOrException().getUUID())) {
                    if (!(currentChannel instanceof CustomChannel channel) || !channel.getChannelName().equals(channelName)) {
                        newChannel = CustomChannels.getChannel(channelName);
                    }
                } else {
                    MessageUtil.sendErrorMessage(ctx.getSource(), "チャンネル " + channelName + " は存在しません");
                    return 2;
                }
            }
            case PRIVATE -> {
                UUID target = null;
                EntitySelector selector = BrigadierUtil.getArgumentOrDefault(ctx, EntitySelector.class, "対象", null);
                if (selector != null) {
                    try {
                        target = selector.findSinglePlayer(ctx.getSource()).getUUID();
                    } catch (CommandSyntaxException e) {
                        target = Bukkit.getPlayerUniqueId(StringArgumentType.getString(ctx, "対象"));
                    }
                }

                if (target == null) {
                    MessageUtil.sendErrorMessage(ctx.getSource(), "プレイヤーが見つかりませんでした");
                    return 2;
                } else {
                    if (!(currentChannel instanceof PrivateChatChannel p)) newChannel = new PrivateChatChannel(target);
                    else {
                        if (!p.getTarget().equals(target)) {
                            p.setTarget(target);
                            newChannel = p;
                        }
                    }
                }
            }
        }

        if (newChannel == null) {
            MessageUtil.sendErrorMessage(ctx.getSource(), "デフォルトの発言先は変更されませんでした");
            return 1;
        }

        ChatManager.setChannel(ctx.getSource().getPlayerOrException().getUUID(), newChannel);
        if (newChannel instanceof CustomChannel channel) NewMessageUtil.sendMessage(ctx.getSource(),
                Component.literal("デフォルトの発言先を ")
                        .append(NewMessageUtil.convertAdventure2Minecraft(channel.getDisplayName()))
                        .append(Component.literal("(" + channel.getChannelName() + ")")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                        .append(" に変更しました"));
        else MessageUtil.sendMessage(ctx.getSource(), "デフォルトの発言先を " + newChannel.getChannelName() + " に変更しました");
        return 0;
    }

    private static int createChannel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (ctx.getSource().getEntity() != null && ctx.getSource().getEntity() instanceof Player player) {
            String internalChannelName = StringArgumentType.getString(ctx, "チャンネル名");
            if (CustomChannels.isChannelFound(internalChannelName)) {
                MessageUtil.sendErrorMessage(ctx.getSource(), "チャンネル名 " + internalChannelName + " は既に使用されています");
                return 1;
            }

            Component displayName$minecraft = ComponentArgument.getComponent(ctx, "表示名");
            net.kyori.adventure.text.Component displayName$adv = NewMessageUtil.convertMinecraft2Adventure(displayName$minecraft);
            CustomChannel channel = CustomChannels.createChannel(internalChannelName, player.getUUID(), displayName$adv);

            NewMessageUtil.sendMessage(ctx.getSource(),
                    Component.literal("チャンネル ")
                            .append(NewMessageUtil.convertAdventure2Minecraft(channel.getDisplayName()))
                            .append(Component.literal("(" + channel.getChannelName() + ")")
                                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                            .append("を作成しました"), true);

            return ChannelCommand.setChannel(ctx, ChannelType.CUSTOM);
        }
        return 0;
    }

    private static int removeChannel(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() != null && ctx.getSource().getEntity() instanceof Player player) {
            String channelName = BrigadierUtil.getArgumentOrDefault(ctx, String.class, "チャンネル名", null);
            if (channelName == null) {
                ChatChannel channel = ChatManager.getCurrentChannel(player.getUUID());
                if (channel.getType() == ChannelType.CUSTOM && channel instanceof CustomChannel customChannel) {
                    channelName = customChannel.getChannelName();
                } else {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("システムチャンネルを削除することはできません"));
                    return 1;
                }
            }

            if (CustomChannels.isChannelFound(channelName)) {
                CustomChannel channel = CustomChannels.getChannel(channelName);
                if (channel.getOwner().equals(player.getUUID())) {
                    if (TO_REMOVE_CONFIRM.containsKey(player.getUUID()) && TO_REMOVE_CONFIRM.get(player.getUUID()).getChannelName().equals(channelName)) {
                        TO_REMOVE_CONFIRM.remove(player.getUUID());
                        CustomChannels.removeChannel(channelName);
                        NewMessageUtil.sendMessage(ctx.getSource(), Component.literal("チャンネル ")
                                .append(NewMessageUtil.convertAdventure2Minecraft(channel.getDisplayName()))
                                .append(Component.literal(" が削除されました")));
                        return 0;
                    } else {
                        TO_REMOVE_CONFIRM.put(player.getUUID(), channel);
                        NewMessageUtil.sendMessage(ctx.getSource(), Component.literal("チャンネル ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                                .append(NewMessageUtil.convertAdventure2Minecraft(channel.getDisplayName()))
                                .append(Component.literal(" を本当に削除しますか？"))
                                .append("\n")
                                .append(Component.literal("本当に削除する場合は、もう一度コマンドを実行してください。")));
                        return 0;
                    }
                } else {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("チャンネルのオーナーのみがチャンネルを削除できます"));
                    return 2;
                }
            } else {
                NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("チャンネル " + channelName + " は存在しません"));
                return 3;
            }
        }
        return 0;
    }

    private static int inviteToChannel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        /*
                        .then(Commands.literal("invite") // /channel invite <target: Single> [channelName]
                        .then(Commands.argument("対象", EntityArgument.player())
                                .executes(ChannelCommand::inviteToChannel) // invite defaulting channel (only for CUSTOM_CHANNEL)
                                .then(Commands.argument("チャンネル名", StringArgumentType.word())
                                        .suggests(Suggestions.JOINED_CHANNELS_SUGGEST)
                                        .executes(ChannelCommand::inviteToChannel)))) // invite specified channel
         */
        if (ctx.getSource().getEntity() != null && ctx.getSource().getEntity() instanceof Player player) {
            ServerPlayer inviteTarget = EntityArgument.getPlayer(ctx, "対象");
            String inviteChannelName = BrigadierUtil.getArgumentOrDefault(ctx, String.class, "チャンネル名", null);
            if (inviteChannelName != null && !CustomChannels.isChannelFound(inviteChannelName)) {
                MessageUtil.sendErrorMessage(ctx.getSource(), "チャンネル " + inviteChannelName + " は存在しません");
                return 1;
            }

            CustomChannel inviteChannel = CustomChannels.getChannel(inviteChannelName);
            if (inviteChannel == null) {
                ChatChannel channel = ChatManager.getCurrentChannel(player.getUUID());
                if (channel.getType() == ChannelType.CUSTOM && channel instanceof CustomChannel) {
                    inviteChannel = (CustomChannel) channel;
                    inviteChannelName = inviteChannel.getChannelName();
                } else {
                    MessageUtil.sendErrorMessage(ctx.getSource(), "カスタムチャンネル以外には招待できません！チャンネル名を指定するか、デフォルトチャンネルを切り替えてください。");
                    return 2;
                }
            }

            /* Pre-flight Check */
            if (CHANNEL_INVITES.containsKey(inviteTarget.getUUID())) {
                Map<UUID, Map<BukkitTask, CustomChannel>> invites = CHANNEL_INVITES.get(inviteTarget.getUUID());
                if (invites.containsKey(player.getUUID())) {
                    Map<BukkitTask, CustomChannel> invitedChannels = invites.get(player.getUUID());
                    if (invitedChannels.containsValue(inviteChannel)) {
                        MessageUtil.sendErrorMessage(ctx.getSource(), "既に " + inviteTarget.getScoreboardName() + " をチャンネル " + inviteChannelName + " に招待しています");
                        return 3;
                    }
                }
            }

            if (inviteChannel == null) {
                MessageUtil.sendErrorMessage(ctx.getSource(), "招待先チャンネルが見つかりませんでした。");
                return 4;
            }

            Component displayName$minecraft = NewMessageUtil.convertAdventure2Minecraft(inviteChannel.getDisplayName());

            if (inviteChannel.getPlayers().contains(inviteTarget.getUUID())) {
                NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("")
                        .append(inviteTarget.getName())
                        .append(Component.literal(" は既にチャンネル "))
                        .append(displayName$minecraft)
                        .append(Component.literal("(" + inviteChannel.getChannelName() + ")").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                        .append(Component.literal(" に参加しています")));
                return 5;
            }

            final CustomChannel finalInviteChannel = inviteChannel;
            BukkitTask inviteExpiredTask = RunnableManager.runAsyncDelayed(() -> {
                Map<BukkitTask, CustomChannel> invites = CHANNEL_INVITES.get(inviteTarget.getUUID()).get(player.getUUID());
                invites.entrySet().removeIf(e -> e.getValue().equals(finalInviteChannel) || e.getValue().getChannelName().equals(finalInviteChannel.getChannelName()));

                inviteTarget.sendSystemMessage(Component.literal("").withStyle(ChatFormatting.RED)
                        .append(player.getName())
                        .append(Component.literal(" からのチャンネル "))
                        .append(displayName$minecraft)
                        .append(Component.literal(" への招待は期限切れとなりました")));

                player.sendSystemMessage(Component.literal("").withStyle(ChatFormatting.RED)
                        .append(inviteTarget.getName())
                        .append(Component.literal(" へのチャンネル "))
                        .append(displayName$minecraft)
                        .append(Component.literal(" への招待は期限切れとなりました")));
            }, (20 * 60) * 5);

            Map<UUID, Map<BukkitTask, CustomChannel>> inviter2invites = CHANNEL_INVITES.getOrDefault(inviteTarget.getUUID(), new HashMap<>());
            Map<BukkitTask, CustomChannel> invites = inviter2invites.getOrDefault(player.getUUID(), new HashMap<>());
            invites.put(inviteExpiredTask, inviteChannel);
            inviter2invites.put(player.getUUID(), invites);
            CHANNEL_INVITES.put(inviteTarget.getUUID(), inviter2invites);

            inviteTarget.sendSystemMessage(Component.literal("")
                    .append(player.getName())
                    .append(Component.literal(" にチャンネル "))
                    .append(displayName$minecraft)
                    .append(Component.literal("(" + inviteChannelName + ")").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                    .append(Component.literal(" に招待されました！\n"))
                    .append(Component.literal("ここをクリックして承諾").setStyle(Style.EMPTY
                            .withColor(TextColor.fromLegacyFormat(ChatFormatting.AQUA))
                            .withBold(true)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/channel accept " + player.getScoreboardName() + " " + inviteChannelName)))));


            NewMessageUtil.sendMessage(ctx.getSource(), Component.literal("")
                    .append(inviteTarget.getScoreboardName())
                    .append(Component.literal(" にチャンネル "))
                    .append(NewMessageUtil.convertAdventure2Minecraft(inviteChannel.getDisplayName()))
                    .append(Component.literal("(" + inviteChannel.getChannelName() + ")"))
                    .append(Component.literal(" への招待を送信しました")));
        }
        return 0;
    }

    private static int acceptInvite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (ctx.getSource().getEntity() != null && ctx.getSource().getEntity() instanceof Player player) {
            if (!CHANNEL_INVITES.containsKey(player.getUUID())) {
                MessageUtil.sendErrorMessage(ctx.getSource(), "チャンネルへの招待は届いていません");
                return 1;
            }

            Player inviteSrc = BrigadierUtil.isArgumentKeyExists(ctx, "対象") ? EntityArgument.getPlayer(ctx, "対象") : null;
            UUID inviterUniqueId = inviteSrc == null ? null : inviteSrc.getUUID();
            String channelName = BrigadierUtil.getArgumentOrDefault(ctx, String.class, "チャンネル名", null);
            Map<UUID, Map<BukkitTask, CustomChannel>> inviter2invites = CHANNEL_INVITES.get(player.getUUID());

            if (inviter2invites.size() == 0) {
                MessageUtil.sendErrorMessage(ctx.getSource(), "チャンネルへの招待は届いていません");
                return 1;
            }

            BukkitTask key = null;
            if (inviteSrc != null) {
                if (!inviter2invites.containsKey(inviteSrc.getUUID())) {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("")
                            .append(inviteSrc.getDisplayName())
                            .append(Component.literal(" からのチャンネルへの招待は届いていません")));
                    return 1;
                }

                if (channelName != null) {
                    if (!CustomChannels.isChannelFound(channelName)) {
                        NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("チャンネル " + channelName + " は存在しません"));
                        return 2;
                    }

                    Optional<Map.Entry<BukkitTask, CustomChannel>> optionalEntry = inviter2invites.get(inviteSrc.getUUID())
                            .entrySet()
                            .stream()
                            .filter(e -> e.getValue().getChannelName().equals(channelName))
                            .findFirst();

                    if (optionalEntry.isPresent()) {
                        key = optionalEntry.get().getKey();
                    } else {
                        NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("")
                                .append(inviteSrc.getDisplayName())
                                .append(Component.literal(" からチャンネル " + channelName + " への招待は届いていません")));
                        return 1;
                    }
                }
            }

            Map<BukkitTask, CustomChannel> invites;
            if (inviteSrc == null) {
                Optional<UUID> optionalUUID = inviter2invites.entrySet()
                        .stream()
                        .filter(e -> e.getValue().size() > 0)
                        .map(Map.Entry::getKey)
                        .findFirst();

                if (optionalUUID.isPresent()) {
                    invites = inviter2invites.get(optionalUUID.get());
                    inviterUniqueId = optionalUUID.get();
                } else {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("チャンネルへの招待は届いていません"));
                    return 1;
                }
            } else {
                invites = inviter2invites.getOrDefault(inviteSrc.getUUID(), new HashMap<>());
            }

            if (key == null) {
                Optional<BukkitTask> optionalKey = invites.keySet().stream().findFirst();
                if (optionalKey.isPresent()) {
                    key = optionalKey.get();
                } else {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("チャンネルへの招待が届いているはずでしたが、届いていませんでした。"));
                    return 1;
                }
            }

            CustomChannel channel = invites.get(key);
            key.cancel();
            channel.addPlayer(player.getUUID());
            invites.remove(key);
            Component name;
            OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(inviterUniqueId);
            if (oPlayer.isOnline()) name = ((CraftPlayer) oPlayer).getHandle().getDisplayName();
            else name = Component.literal(oPlayer.getName());

            NewMessageUtil.sendMessage(ctx.getSource(), Component.literal("")
                    .append(name)
                    .append(Component.literal(" からのチャンネル "))
                    .append(NewMessageUtil.convertAdventure2Minecraft(channel.getDisplayName()))
                    .append(Component.literal("(" + channel.getChannelName() + ")")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                    .append(Component.literal(" への招待を承諾しました")));
        }
        return 0;
    }

    private static int denyInvite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (ctx.getSource().getEntity() != null && ctx.getSource().getEntity() instanceof Player player) {
            if (!CHANNEL_INVITES.containsKey(player.getUUID())) {
                MessageUtil.sendErrorMessage(ctx.getSource(), "チャンネルへの招待は届いていません");
                return 1;
            }

            Player inviteSrc = BrigadierUtil.isArgumentKeyExists(ctx, "対象") ? EntityArgument.getPlayer(ctx, "対象") : null;
            UUID inviterUniqueId = inviteSrc == null ? null : inviteSrc.getUUID();
            String channelName = BrigadierUtil.getArgumentOrDefault(ctx, String.class, "チャンネル名", null);
            Map<UUID, Map<BukkitTask, CustomChannel>> inviter2invites = CHANNEL_INVITES.get(player.getUUID());

            if (inviter2invites.size() == 0) {
                MessageUtil.sendErrorMessage(ctx.getSource(), "チャンネルへの招待は届いていません");
                return 1;
            }

            BukkitTask key = null;
            if (inviteSrc != null) {
                if (!inviter2invites.containsKey(inviteSrc.getUUID())) {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("")
                            .append(inviteSrc.getDisplayName())
                            .append(Component.literal(" からのチャンネルへの招待は届いていません")));
                    return 1;
                }

                if (channelName != null) {
                    if (!CustomChannels.isChannelFound(channelName)) {
                        NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("チャンネル " + channelName + " は存在しません"));
                        return 2;
                    }

                    Optional<Map.Entry<BukkitTask, CustomChannel>> optionalEntry = inviter2invites.get(inviteSrc.getUUID())
                            .entrySet()
                            .stream()
                            .filter(e -> e.getValue().getChannelName().equals(channelName))
                            .findFirst();

                    if (optionalEntry.isPresent()) {
                        key = optionalEntry.get().getKey();
                    } else {
                        NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("")
                                .append(inviteSrc.getDisplayName())
                                .append(Component.literal(" からチャンネル " + channelName + " への招待は届いていません")));
                        return 1;
                    }
                }
            }

            Map<BukkitTask, CustomChannel> invites;
            if (inviteSrc == null) {
                Optional<UUID> optionalUUID = inviter2invites.entrySet()
                        .stream()
                        .filter(e -> e.getValue().size() > 0)
                        .map(Map.Entry::getKey)
                        .findFirst();

                if (optionalUUID.isPresent()) {
                    invites = inviter2invites.get(optionalUUID.get());
                    inviterUniqueId = optionalUUID.get();
                } else {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("チャンネルへの招待は届いていません"));
                    return 1;
                }
            } else {
                invites = inviter2invites.getOrDefault(inviteSrc.getUUID(), new HashMap<>());
            }

            if (key == null) {
                Optional<BukkitTask> optionalKey = invites.entrySet().stream().map(Map.Entry::getKey).findFirst();
                if (optionalKey.isPresent()) {
                    key = optionalKey.get();
                } else {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.literal("チャンネルへの招待が届いているはずでしたが、届いていませんでした。"));
                    return 1;
                }
            }

            CustomChannel channel = invites.get(key);
            key.cancel();
            invites.remove(key);
            Component name;
            OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(inviterUniqueId);
            if (oPlayer.isOnline()) name = ((CraftPlayer) oPlayer).getHandle().getDisplayName();
            else name = Component.literal(oPlayer.getName());

            NewMessageUtil.sendMessage(ctx.getSource(), Component.literal("").withStyle(ChatFormatting.RED)
                    .append(name)
                    .append(Component.literal(" からのチャンネル "))
                    .append(NewMessageUtil.convertAdventure2Minecraft(channel.getDisplayName()))
                    .append(Component.literal("(" + channel.getChannelName() + ")")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                    .append(Component.literal(" への招待を拒否しました")));

            if (oPlayer.isOnline()) {
                NewMessageUtil.sendMessage(((org.bukkit.entity.Player) oPlayer), Component.literal("").withStyle(ChatFormatting.RED)
                        .append(player.getDisplayName())
                        .append(Component.literal(" はチャンネル "))
                        .append(NewMessageUtil.convertAdventure2Minecraft(channel.getDisplayName()))
                        .append(Component.literal("(" + channel.getChannelName() + ")")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                        .append(Component.literal(" への招待を拒否しました")));
            }
        }
        return 0;
    }

    private static int leaveChannel(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() != null && ctx.getSource().getEntity() instanceof Player player) {
            String channelName = BrigadierUtil.getArgumentOrDefault(ctx, String.class, "チャンネル名", null);
            if (channelName == null) {
                ChatChannel channel = ChatManager.getCurrentChannel(player.getUUID());
                if (channel.getType() == ChannelType.CUSTOM && channel instanceof CustomChannel) {
                    channelName = channel.getChannelName();
                } else {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), "システムチャンネルから退出することはできません");
                    return 1;
                }
            }

            if (!CustomChannels.isChannelFound(channelName)) {
                NewMessageUtil.sendErrorMessage(ctx.getSource(), "チャンネル " + channelName + " は存在しません");
                return 2;
            }

            CustomChannel channel = CustomChannels.getChannel(channelName);

            if (channel.getPlayers().contains(player.getUUID())) {
                channel.removePlayer(player.getUUID());
                NewMessageUtil.sendMessage(ctx.getSource(), Component.literal("チャンネル ")
                        .append(NewMessageUtil.convertAdventure2Minecraft(channel.getDisplayName()))
                        .append(Component.literal("(" + channel.getChannelName() + ")").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                        .append(Component.literal(" から退出しました")));
            } else {
                NewMessageUtil.sendErrorMessage(ctx.getSource(), "参加していないチャンネルから退出することはできません");
                return 3;
            }
        }
        return 0;
    }

    private static int showGlobalOption(CommandContext<CommandSourceStack> ctx, GlobalOptions type, boolean set) throws CommandSyntaxException {
        Object value = type.getValue(ctx.getSource().getPlayerOrException().getUUID());
        NewMessageUtil.sendMessage(ctx.getSource(), type.getOptionName() + " " + (set ? "が" : "は") + " " + (value instanceof Boolean b ? (b ? "有効" : "無効") : value.toString()) + " に設定され" + (set ? "ました" : "ています"));
        return 0;
    }

    private static int setGlobalOption(CommandContext<CommandSourceStack> ctx, GlobalOptions type) throws CommandSyntaxException {
        type.setValue(ctx.getSource().getPlayerOrException().getUUID(), type.getArgument(ctx));
        showGlobalOption(ctx, type, true);
        return 0;
    }

    private enum GlobalOptions {
        FORCE_GLOBAL_CHAT_PREFIX("全体強制送信接頭辞") {
            @Override
            public Object getArgument(CommandContext<CommandSourceStack> ctx) {
                return StringArgumentType.getString(ctx, "接頭辞");
            }

            @Override
            public Object getValue(UUID uniqueId) {
                return PlayerData.of(uniqueId).getChatData().getForceGlobalChatPrefix();
            }

            @Override
            public void setValue(UUID uniqueId, Object newValue) {
                if (!(newValue instanceof String))
                    throw new IllegalArgumentException("Required \"String\" but found \"" + newValue.getClass().getName() + "\"");
                PlayerData.of(uniqueId).getChatData().setForceGlobalChatPrefix((String) newValue);
            }
        },
        KANA_CONVERT("ローマ字かな変換") {
            @Override
            public Object getArgument(CommandContext<CommandSourceStack> ctx) {
                return BoolArgumentType.getBool(ctx, "有効");
            }

            @Override
            public Object getValue(UUID uniqueId) {
                return PlayerData.of(uniqueId).getChatData().isUseKanaConvert();
            }

            @Override
            public void setValue(UUID uniqueId, Object newValue) {
                if (!(newValue instanceof Boolean))
                    throw new IllegalArgumentException("Requires \"Boolean\" but found \"" + newValue.getClass().getName() + "\"");
                PlayerData.of(uniqueId).getChatData().setUseKanaConvert((boolean) newValue);
            }
        };

        private final String optionName;

        GlobalOptions(String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return this.optionName;
        }

        public Object getArgument(CommandContext<CommandSourceStack> ctx) {
            return null;
        }

        public Object getValue(UUID uniqueId) {
            return null;
        }

        public void setValue(UUID uniqueId, Object newValue) {
            // nothing here
        }
    }
}
