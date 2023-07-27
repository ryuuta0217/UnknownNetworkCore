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

package net.unknown.core.commands.vanilla;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.chat.CustomChatTypes;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.enums.Permissions;
import net.unknown.core.events.PrivateMessageEvent;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MsgCommand {
    //public static final Set<UUID> MESSAGE_SPY_DISABLED_PLAYERS = new HashSet<>();
    private static final UUID SERVER_UUID = Util.NIL_UUID;
    private static final String MESSAGE_SPY_FORMAT = "§7§o[%1$s → %2$s]§r %3$s";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // FORCE UNREGISTER [minecraft:msg, minecraft:tell, minecraft:w] COMMAND FROM COMMAND DISPATCHER
        BrigadierUtil.forceUnregisterCommand("msg");
        BrigadierUtil.forceUnregisterCommand("tell");
        BrigadierUtil.forceUnregisterCommand("w");
        // FORCE UNREGISTER END

        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("msg");
        builder.requires(Permissions.COMMAND_MSG::check);
        builder.then(Commands.argument("対象", EntityArgument.players())
                .then(Commands.argument("メッセージ", MessageArgument.message())
                        .executes(MsgCommand::executeMsg)));
        LiteralCommandNode<CommandSourceStack> msgNode = dispatcher.register(builder); // msg 登録

        LiteralArgumentBuilder<CommandSourceStack> tellBuilder = LiteralArgumentBuilder.literal("tell");
        tellBuilder.requires(Permissions.COMMAND_MSG::check);
        tellBuilder.redirect(msgNode);
        dispatcher.register(tellBuilder); // tell 登録

        LiteralArgumentBuilder<CommandSourceStack> whisperBuilder = LiteralArgumentBuilder.literal("whisper");
        whisperBuilder.requires(Permissions.COMMAND_MSG::check);
        whisperBuilder.redirect(msgNode);
        dispatcher.register(whisperBuilder); // whisper 登録

        LiteralArgumentBuilder<CommandSourceStack> wBuilder = LiteralArgumentBuilder.literal("w");
        wBuilder.requires(Permissions.COMMAND_MSG::check);
        wBuilder.redirect(msgNode);
        dispatcher.register(wBuilder); // w 登録

        LiteralArgumentBuilder<CommandSourceStack> replyBuilder = LiteralArgumentBuilder.literal("reply");
        replyBuilder.requires(Permissions.COMMAND_REPLY::checkAndIsPlayer);
        replyBuilder.executes(MsgCommand::executeShowReplyTarget)
                .then(Commands.argument("メッセージ", MessageArgument.message())
                        .executes(MsgCommand::executeReply));
        LiteralCommandNode<CommandSourceStack> replyNode = dispatcher.register(replyBuilder); // reply 登録

        LiteralArgumentBuilder<CommandSourceStack> rBuilder = LiteralArgumentBuilder.literal("r");
        rBuilder.requires(Permissions.COMMAND_REPLY::checkAndIsPlayer);
        rBuilder.executes(MsgCommand::executeShowReplyTarget);
        rBuilder.redirect(replyNode);
        dispatcher.register(rBuilder); // r 登録
    }

    private static int executeMsg(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Collection<ServerPlayer> receivers = EntityArgument.getPlayers(ctx, "対象");

        MessageArgument.resolveChatMessage(ctx, "メッセージ", (message) -> {
            ChatType.Bound incomingBound = ChatType.bind(CustomChatTypes.PRIVATE_MESSAGE_INCOMING, ctx.getSource());

            net.kyori.adventure.text.Component adventureMessage = NewMessageUtil.convertMinecraft2Adventure(message.decoratedContent());
            adventureMessage = adventureMessage.replaceText((b) -> { // URL Support
                b.match(Pattern.compile("https?://\\S+")).replacement((r, b2) -> net.kyori.adventure.text.Component.text(b2.content(), net.kyori.adventure.text.format.Style.style(DefinedTextColor.AQUA, TextDecoration.UNDERLINED)).clickEvent(ClickEvent.openUrl(b2.content())));
            });
            message = message.withUnsignedContent(NewMessageUtil.convertAdventure2Minecraft(adventureMessage));
            OutgoingChatMessage outMessage = OutgoingChatMessage.create(message);

            PrivateMessageEvent pEvent = new PrivateMessageEvent(ctx.getSource(), receivers, message);
            Bukkit.getPluginManager().callEvent(pEvent);
            if (pEvent.isCancelled()) return;

            boolean filtered = false;

            for (ServerPlayer receiver : receivers) {
                ChatType.Bound outgoingBound = ChatType.bind(CustomChatTypes.PRIVATE_MESSAGE_OUTGOING, receiver).withTargetName(receiver.getDisplayName());
                source.sendChatMessage(outMessage, false, outgoingBound);
                boolean filterMask = source.shouldFilterMessageTo(receiver);
                receiver.sendChatMessage(outMessage, filterMask, incomingBound);
                receiver.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1);
                PlayerData.of(receiver.getUUID()).getChatData().setPrivateMessageReplyTarget(source.getEntity() != null ? source.getEntity().getUUID() : SERVER_UUID);
                filtered |= filterMask && message.isFullyFiltered();
            }

            if (filtered) {
                source.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
            }
        });
        return 0;
    }

    private static int executeShowReplyTarget(CommandContext<CommandSourceStack> ctx) {
        UUID executorUniqueId = ctx.getSource().getEntity() != null ? ctx.getSource().getEntity().getUUID() : SERVER_UUID;
        String replyTarget = "なし";
        if (PlayerData.of(executorUniqueId).getChatData().getPrivateMessageReplyTarget() != null) {
            UUID replyTargetUniqueId = PlayerData.of(executorUniqueId).getChatData().getPrivateMessageReplyTarget();
            if (replyTargetUniqueId.equals(SERVER_UUID)) {
                replyTarget = "サーバー";
            } else if (Bukkit.getEntity(replyTargetUniqueId) != null && Bukkit.getEntity(replyTargetUniqueId).getType() != EntityType.PLAYER) {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(replyTargetUniqueId);
                replyTarget = String.format("%s (%s, %s,%s,%s)", entity.getType().name(), MessageUtil.getWorldNameDisplay(entity.getWorld()), entity.getLocation().getBlockX(), entity.getLocation().getBlockY(), entity.getLocation().getBlockZ());
            } else {
                OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(replyTargetUniqueId);
                replyTarget = oPlayer.getName() + (!oPlayer.isOnline() ? " (オフライン)" : "");
            }
        }
        MessageUtil.sendMessage(ctx.getSource(), "現在の返信先: " + replyTarget, false);
        return 0;
    }

    private static int executeReply(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        UUID replyTarget = PlayerData.of(source.getPlayerOrException()).getChatData().getPrivateMessageReplyTarget();

        if (replyTarget == null) {
            NewMessageUtil.sendErrorMessage(source, "返信先が見つかりません");
            return 1;
        }

        boolean replyTargetIsServer = replyTarget.equals(Util.NIL_UUID);

        if (!replyTargetIsServer && !Bukkit.getOfflinePlayer(replyTarget).isOnline()) {
            NewMessageUtil.sendErrorMessage(source, "返信先のプレイヤーがオフラインです");
            return 2;
        }

        ServerPlayer receiver = MinecraftAdapter.player(Bukkit.getPlayer(replyTarget));

        MessageArgument.resolveChatMessage(ctx, "メッセージ", (message) -> {
            ChatType.Bound incomingBound = ChatType.bind(CustomChatTypes.PRIVATE_MESSAGE_INCOMING, ctx.getSource());
            OutgoingChatMessage outMessage = OutgoingChatMessage.create(message);

            PrivateMessageEvent pEvent = new PrivateMessageEvent(ctx.getSource(), Collections.singleton(receiver), message);
            Bukkit.getPluginManager().callEvent(pEvent);
            if (pEvent.isCancelled()) return;

            boolean filtered;

            ChatType.Bound outgoingBound = ChatType.bind(CustomChatTypes.PRIVATE_MESSAGE_OUTGOING, receiver).withTargetName(receiver.getDisplayName());
            source.sendChatMessage(outMessage, false, outgoingBound);
            boolean filterMask = source.shouldFilterMessageTo(receiver);
            receiver.sendChatMessage(outMessage, filterMask, incomingBound);
            PlayerData.of(receiver).getChatData().setPrivateMessageReplyTarget(source.getEntity() != null ? source.getEntity().getUUID() : SERVER_UUID);
            filtered = filterMask && message.isFullyFiltered();

            if (filtered) {
                source.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
            }
        });

        return 0;
    }

    public static Component spyMessage(Component senderName, Component receiverName, Component message) {
        return MutableComponent.create(new LiteralContents(""))
                .append(MutableComponent.create(new LiteralContents("[PM]")).setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)))
                .append(" ")
                .append(MutableComponent.create(new LiteralContents("[")).append(senderName).append(" -> ").append(receiverName).append("]").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)))
                .append(" ")
                .append(message);
    }

    private static void spyPrivateMessage(Component from, Component to, String msg) {
        /*Set<Player> receivers = new HashSet<>();

        Bukkit.getPluginManager().getPermissionSubscriptions(Permissions.FEATURE_PRIVATE_MESSAGE_SPY.getPermissionNode()).forEach(permissible -> {
            if (permissible instanceof Player && // 実はこの書き方嫌い (ネストしたほうがよっぽどいい)
                    permissible.hasPermission(Permissions.FEATURE_PRIVATE_MESSAGE_SPY.getPermissionNode()) &&
                    !MESSAGE_SPY_DISABLED_PLAYERS.contains(((Player) permissible).getUUID()) &&
                    !((Player) permissible).getName().equals(from) && !((Player) permissible).getName().equals(to)) {
                receivers.add((Player) permissible);
            }
        });

        receivers.forEach(player -> player.sendMessage(String.format(MESSAGE_SPY_FORMAT, from, to, msg)));*/
    }
}
