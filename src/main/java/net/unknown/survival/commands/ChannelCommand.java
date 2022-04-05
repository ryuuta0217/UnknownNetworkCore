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

package net.unknown.survival.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.survival.chat.ChatManager;
import net.unknown.survival.chat.channels.*;
import net.unknown.survival.chat.channels.ranged.MeChatChannel;
import net.unknown.survival.chat.channels.ranged.NearChatChannel;
import net.unknown.survival.chat.channels.ranged.TitleChatChannel;
import org.bukkit.Bukkit;

import java.util.UUID;

public class ChannelCommand {
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
                                .then(Commands.argument("表示名", StringArgumentType.string())
                                        .executes(ChannelCommand::createChannel))))
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
                                .executes(ChannelCommand::acceptInvite)))
                .then(Commands.literal("deny")
                        .executes(ChannelCommand::denyInvite)
                        .then(Commands.argument("対象", EntityArgument.player())
                                .executes(ChannelCommand::denyInvite)));

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

            }
            case PRIVATE -> {
                UUID target = null;
                EntitySelector selector = BrigadierUtil.getArgumentOrDefault(ctx, EntitySelector.class, "対象", null);
                System.out.println("SELECTOR* " + selector);
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
        MessageUtil.sendMessage(ctx.getSource(), "デフォルトの発言先を " + newChannel.getChannelName() + " に変更しました");
        return 0;
    }

    private static int createChannel(CommandContext<CommandSourceStack> ctx) {
        return 0;
    }

    private static int inviteToChannel(CommandContext<CommandSourceStack> ctx) {
        return 0;
    }

    private static int acceptInvite(CommandContext<CommandSourceStack> ctx) {
        return 0;
    }

    private static int denyInvite(CommandContext<CommandSourceStack> ctx) {
        return 0;
    }

    private static int leaveChannel(CommandContext<CommandSourceStack> ctx) {
        return 0;
    }
}
