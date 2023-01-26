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

package net.unknown.core.util;

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.spigotmc.SpigotConfig;

public class NewMessageUtil {
    /* NORMAL MESSAGES */
    /* START - Minecraft Components & ROOT */
    public static void sendMessage(CommandSourceStack source, Component component, boolean broadcastToOps) {
        source.sendSuccess(NewMessageUtil.format(component), false);
        if (broadcastToOps) broadcastCommandFeedback(component, source, false);
    }

    public static void sendMessage(CommandSourceStack source, Component component) {
        sendMessage(source, component, true);
    }

    public static void sendMessage(Player player, Component component, boolean broadcastToOps) {
        sendMessage(player.createCommandSourceStack(), component, broadcastToOps);
    }

    public static void sendMessage(Player player, Component component) {
        sendMessage(player, component, true);
    }

    public static void sendMessage(org.bukkit.entity.Player player, Component component, boolean broadcastToOps) {
        sendMessage(((CraftPlayer) player).getHandle(), component, broadcastToOps);
    }

    public static void sendMessage(org.bukkit.entity.Player player, Component component) {
        sendMessage(player, component, true);
    }

    public static void sendMessage(org.bukkit.entity.HumanEntity human, Component component, boolean broadcastToOps) {
        sendMessage(((CraftHumanEntity) human).getHandle(), component, broadcastToOps);
    }

    public static void sendMessage(org.bukkit.entity.HumanEntity human, Component component) {
        sendMessage(human, component, true);
    }
    /* END - Minecraft Components */

    /* START - Adventure Components */
    public static void sendMessage(CommandSourceStack source, net.kyori.adventure.text.Component component, boolean broadcastToOps) {
        sendMessage(source, convertAdventure2Minecraft(component), broadcastToOps);
    }

    public static void sendMessage(CommandSourceStack source, net.kyori.adventure.text.Component component) {
        sendMessage(source, component, true);
    }

    public static void sendMessage(Player player, net.kyori.adventure.text.Component component, boolean broadcastToOps) {
        sendMessage(player.createCommandSourceStack(), component, broadcastToOps);
    }

    public static void sendMessage(Player player, net.kyori.adventure.text.Component component) {
        sendMessage(player, component, true);
    }

    public static void sendMessage(org.bukkit.entity.Player player, net.kyori.adventure.text.Component component, boolean broadcastToOps) {
        sendMessage(((CraftPlayer) player).getHandle(), component, broadcastToOps);
    }

    public static void sendMessage(org.bukkit.entity.Player player, net.kyori.adventure.text.Component component) {
        sendMessage(player, component, true);
    }

    public static void sendMessage(org.bukkit.entity.HumanEntity human, net.kyori.adventure.text.Component component, boolean broadcastToOps) {
        sendMessage(((CraftHumanEntity) human).getHandle(), component, broadcastToOps);
    }

    public static void sendMessage(org.bukkit.entity.HumanEntity human, net.kyori.adventure.text.Component component) {
        sendMessage(human, component, true);
    }
    /* END - Adventure Components */

    /* START - Plain Texts */
    public static void sendMessage(CommandSourceStack source, String message, boolean broadcastToOps) {
        sendMessage(source, MutableComponent.create(new LiteralContents(message)), broadcastToOps);
    }

    public static void sendMessage(CommandSourceStack source, String message) {
        sendMessage(source, message, true);
    }

    public static void sendMessage(Player player, String message, boolean broadcastToOps) {
        sendMessage(player.createCommandSourceStack(), message, broadcastToOps);
    }

    public static void sendMessage(Player player, String message) {
        sendMessage(player, message, true);
    }

    public static void sendMessage(org.bukkit.entity.Player player, String message, boolean broadcastToOps) {
        sendMessage(((CraftPlayer) player).getHandle(), message, broadcastToOps);
    }

    public static void sendMessage(org.bukkit.entity.Player player, String message) {
        sendMessage(player, message, true);
    }

    public static void sendMessage(org.bukkit.entity.HumanEntity human, String message, boolean broadcastToOps) {
        sendMessage(((CraftHumanEntity) human).getHandle(), message, broadcastToOps);
    }

    public static void sendMessage(org.bukkit.entity.HumanEntity human, String message) {
        sendMessage(human, message, true);
    }
    /* END - Plain Texts*/
    /* NORMAL MESSAGES END */

    /* ERROR MESSAGES */
    /* START - Minecraft Components */
    public static void sendErrorMessage(CommandSourceStack source, Component component, boolean broadcastToOps) {
        source.sendFailure(component);
        if (broadcastToOps) broadcastCommandFeedback(component, source, true);
    }

    public static void sendErrorMessage(CommandSourceStack source, Component component) {
        sendErrorMessage(source, component, false);
    }

    public static void sendErrorMessage(Player player, Component component, boolean broadcastToOps) {
        sendErrorMessage(player.createCommandSourceStack(), component, broadcastToOps);
    }

    public static void sendErrorMessage(Player player, Component component) {
        sendErrorMessage(player, component, false);
    }

    public static void sendErrorMessage(org.bukkit.entity.Player player, Component component, boolean broadcastToOps) {
        sendErrorMessage(((CraftPlayer) player).getHandle(), component, broadcastToOps);
    }

    public static void sendErrorMessage(org.bukkit.entity.Player player, Component component) {
        sendErrorMessage(player, component, false);
    }

    public static void sendErrorMessage(org.bukkit.entity.HumanEntity human, Component component, boolean broadcastToOps) {
        sendErrorMessage(((CraftHumanEntity) human).getHandle(), component, broadcastToOps);
    }

    public static void sendErrorMessage(org.bukkit.entity.HumanEntity human, Component component) {
        sendErrorMessage(human, component, false);
    }
    /* END - Minecraft Components */

    /* START - Adventure Components */
    public static void sendErrorMessage(CommandSourceStack source, net.kyori.adventure.text.Component component, boolean broadcastToOps) {
        sendErrorMessage(source, convertAdventure2Minecraft(component), broadcastToOps);
    }

    public static void sendErrorMessage(CommandSourceStack source, net.kyori.adventure.text.Component component) {
        sendErrorMessage(source, component, false);
    }

    public static void sendErrorMessage(Player player, net.kyori.adventure.text.Component component, boolean broadcastToOps) {
        sendErrorMessage(player, convertAdventure2Minecraft(component), broadcastToOps);
    }

    public static void sendErrorMessage(Player player, net.kyori.adventure.text.Component component) {
        sendErrorMessage(player, component, false);
    }

    public static void sendErrorMessage(org.bukkit.entity.Player player, net.kyori.adventure.text.Component component, boolean broadcastToOps) {
        sendErrorMessage(player, convertAdventure2Minecraft(component), broadcastToOps);
    }

    public static void sendErrorMessage(org.bukkit.entity.Player player, net.kyori.adventure.text.Component component) {
        sendErrorMessage(player, component, false);
    }

    public static void sendErrorMessage(org.bukkit.entity.HumanEntity human, net.kyori.adventure.text.Component component, boolean broadcastToOps) {
        sendErrorMessage(human, convertAdventure2Minecraft(component), broadcastToOps);
    }

    public static void sendErrorMessage(org.bukkit.entity.HumanEntity human, net.kyori.adventure.text.Component component) {
        sendErrorMessage(human, component, false);
    }
    /* END - Adventure Components */

    /* START - Plain Texts */
    public static void sendErrorMessage(CommandSourceStack source, String message, boolean broadcastToOps) {
        sendErrorMessage(source, MutableComponent.create(new LiteralContents(message)), broadcastToOps);
    }

    public static void sendErrorMessage(CommandSourceStack source, String message) {
        sendErrorMessage(source, message, false);
    }

    public static void sendErrorMessage(Player player, String message, boolean broadcastToOps) {
        sendErrorMessage(player.createCommandSourceStack(), message, broadcastToOps);
    }

    public static void sendErrorMessage(Player player, String message) {
        sendErrorMessage(player, message, false);
    }

    public static void sendErrorMessage(org.bukkit.entity.Player player, String message, boolean broadcastToOps) {
        sendErrorMessage(((CraftPlayer) player).getHandle(), message, broadcastToOps);
    }

    public static void sendErrorMessage(org.bukkit.entity.Player player, String message) {
        sendErrorMessage(player, message, false);
    }

    public static void sendErrorMessage(org.bukkit.entity.HumanEntity human, String message, boolean broadcastToOps) {
        sendErrorMessage(((CraftHumanEntity) human).getHandle(), message, broadcastToOps);
    }

    public static void sendErrorMessage(org.bukkit.entity.HumanEntity human, String message) {
        sendErrorMessage(human, message, false);
    }
    /* END - Plain Texts*/
    /* ERROR MESSAGES END */

    //public static void broadcast(Component message, UUID sender, ChatType type, String permission) {
    //CraftServer bukkit = (CraftServer) Bukkit.getServer();
    //bukkit.getServer().getPlayerList().broadcastMessage();
    //}

    private static void broadcastCommandFeedback(Component component, CommandSourceStack source, boolean error) {
        // commandBlockOutput: false の時にコマンドブロックからfeedbackが出ることを防ぐが、エラーの時は無視する
        if (!error && !source.source.shouldInformAdmins()) return;
        MutableComponent msg = MutableComponent.create(new TranslatableContents("chat.type.admin", source.getDisplayName(), component))
                .withStyle(error ? ChatFormatting.RED : ChatFormatting.GRAY, ChatFormatting.ITALIC);

        source.getServer().getPlayerList().getPlayers().forEach(player -> {
            if (player != source.source && player.getBukkitEntity().hasPermission("minecraft.admin.command_feedback")) {
                // TODO iterateの前にチェックを挟むか？
                //  前にチェックを挟むと、
                //  ワールドA(sendCommandFeedback: false) で実行されたコマンドがワールドB(sendCommandFeedback: true)のワールドで表示される
                //  → プレイヤーの行動追跡に若干の難が生まれる？
                //  ただし、実行者のワールドAがtrueでも受信者のいるワールドBがfalseだとフィードバックを受信できない
                if (player.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
                    player.sendSystemMessage(msg);
                }
            }
        });

        if (source.source != source.getServer() && source.getServer().getGameRules().getBoolean(GameRules.RULE_LOGADMINCOMMANDS) && !SpigotConfig.silentCommandBlocks) {
            source.getServer().sendSystemMessage(msg);
        }
    }

    private static Component format(Component component) {
        return MutableComponent.create(new LiteralContents(""))
                .append(MutableComponent.create(new LiteralContents("[")).withStyle(ChatFormatting.GRAY))
                .append(MutableComponent.create(new LiteralContents("U.N.")).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(MutableComponent.create(new LiteralContents("]")).withStyle(ChatFormatting.GRAY))
                .append(MutableComponent.create(new LiteralContents(" ")))
                .append(component);
    }

    private static net.kyori.adventure.text.Component format(net.kyori.adventure.text.Component component) {
        return convertMinecraft2Adventure(format(convertAdventure2Minecraft(component)));
        /*return net.kyori.adventure.text.Component.empty()
                .append(net.kyori.adventure.text.Component.text("[", DefinedTextColor.GRAY))
                .append(net.kyori.adventure.text.Component.text("U.N.", net.kyori.adventure.text.format.Style.style(DefinedTextColor.GOLD, TextDecoration.BOLD.withState(true))))
                .append(net.kyori.adventure.text.Component.text("]", DefinedTextColor.GRAY))
                .append(net.kyori.adventure.text.Component.text(" "))
                .append(component);*/
    }

    public static Component convertAdventure2Minecraft(net.kyori.adventure.text.Component component) {
        return Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(component));
    }

    public static net.kyori.adventure.text.Component convertMinecraft2Adventure(Component component) {
        return GsonComponentSerializer.gson().deserializeFromTree(Component.Serializer.toJsonTree(component));
    }

    public static boolean equalsComponent(net.kyori.adventure.text.Component a, net.kyori.adventure.text.Component b) {
        GsonComponentSerializer gson = GsonComponentSerializer.gson();
        return gson.serializeToTree(a).equals(gson.serializeToTree(b));
        //return false;
    }
}
