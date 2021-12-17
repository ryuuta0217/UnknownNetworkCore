/*
 * Copyright (c) 2021 Unknown Network Developers and contributors.
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
 *     loss of use data or profits; or business interpution) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.core.util;

import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

import java.text.SimpleDateFormat;
import java.util.*;

public class MessageUtil {
    private static final String PREFIX = "§r";
    private static final String PREFIX_ERROR = "§c";
    private static final String PREFIX_ADMIN = "§r";
    private static final String PREFIX_ADMIN_ERROR = "§c";

    public static void sendMessage(Player player, String msg) {
        sendMessage(((CraftPlayer) player).getHandle().createCommandSourceStack(), msg, true);
    }

    public static void sendMessage(CommandSourceStack commandSourceStack, String msg) {
        sendMessage(commandSourceStack, msg, true);
    }

    public static void sendMessage(CommandSourceStack commandSourceStack, String msg, boolean showOthers) {
        commandSourceStack.sendSuccess(new TextComponent(PREFIX + msg.replace("\n", "\n" + PREFIX)), false);
        if (showOthers) {
            String msgFormat = String.format("§7§o[%s: %s]", commandSourceStack.getTextName(), ChatColor.stripColor(msg));
            broadcast(msgFormat, "minecraft.admin.command_feedback", commandSourceStack);
        }
    }

    public static void sendErrorMessage(Player player, String msg) {
        player.sendMessage(PREFIX_ERROR + msg);
    }

    public static void sendErrorMessage(CommandSourceStack commandSourceStack, String msg) {
        sendErrorMessage(commandSourceStack, msg, false);
    }

    public static void sendErrorMessage(CommandSourceStack commandSourceStack, String msg, boolean showOthers) {
        commandSourceStack.sendSuccess(new TextComponent(PREFIX_ERROR + msg.replace("\n", "\n" + PREFIX_ERROR)).setStyle(Style.EMPTY.withColor(ChatFormatting.RED)), false);
        if (showOthers) {
            String msgFormat = String.format("§7§o[%s: §c%s§7]", commandSourceStack.getTextName(), ChatColor.stripColor(msg));
            broadcast(msgFormat, "minecraft.admin.command_feedback", commandSourceStack);
        }
    }

    public static void sendAdminMessage(Player player, String msg) {
        player.sendMessage(PREFIX_ADMIN + msg);
    }

    public static void sendAdminMessage(CommandSourceStack commandSourceStack, String msg) {
        sendAdminMessage(commandSourceStack, msg, true);
    }

    public static void sendAdminMessage(CommandSourceStack commandSourceStack, String msg, boolean showOthers) {
        commandSourceStack.sendSuccess(new TextComponent(PREFIX_ADMIN + msg.replace("\n", "\n" + PREFIX_ADMIN)), false);
        if (showOthers) {
            String msgFormat = String.format("§7§o[%s: %s]", commandSourceStack.getTextName(), ChatColor.stripColor(msg));
            broadcast(msgFormat, "minecraft.admin.command_feedback", commandSourceStack);
        }
    }

    public static void sendAdminErrorMessage(Player player, String msg) {
        player.sendMessage(PREFIX_ADMIN_ERROR + msg);
    }

    public static void sendAdminErrorMessage(CommandSourceStack commandSourceStack, String msg) {
        sendAdminErrorMessage(commandSourceStack, msg, false);
    }

    public static void sendAdminErrorMessage(CommandSourceStack commandSourceStack, String msg, boolean showOthers) {
        commandSourceStack.sendSuccess(new TextComponent(PREFIX_ADMIN_ERROR + msg.replace("\n", "\n" + PREFIX_ADMIN_ERROR)).withStyle(Style.EMPTY.withColor(ChatFormatting.RED)), false);
        if (showOthers) {
            String msgFormat = String.format("§7§o[%s: §c%s§7]", commandSourceStack.getTextName(), ChatColor.stripColor(msg));
            broadcast(msgFormat, "minecraft.admin.command_feedback", commandSourceStack);
        }
    }

    public static void broadcast(Component message, UUID broadcaster) {
        broadcast(message, broadcaster, true);
    }

    public static void broadcast(Component message, UUID broadcaster, boolean prefix) {
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(broadcaster == null ? Identity.nil() : Identity.identity(broadcaster), Component.text(PREFIX).append(message));
        });
    }

    public static void broadcast(String message, String permission, CommandSourceStack sentBy) {
        Set<CommandSender> recipients = new HashSet<>();

        for (Permissible permissible : Bukkit.getPluginManager().getPermissionSubscriptions(permission)) {
            if (permissible instanceof CommandSender && permissible.hasPermission(permission)) {
                if (!sentBy.getBukkitSender().getName().equals(((CommandSender) permissible).getName())) {
                    recipients.add((CommandSender) permissible);
                }
            }
        }

        for (CommandSender recipient : recipients) {
            recipient.sendMessage(message);
        }
    }

    public static String getWorldNameDisplay(World world) {
        return world.getName()
                .replace("main2", "メイン2")
                .replace("world_the_end", "メインエンド")
                .replace("world_nether", "メインネザー")
                .replace("world", "メイン")
                .replace("shop", "ショップ")
                .replace("market", "マーケット")
                .replace("resource", "資源")
                .replace("hellowork", "ハローワーク")
                .replace("tutorial", "チュートリアル") + "ワールド";
    }

    public static String getMessagePrefix() {
        return PREFIX;
    }

    public static String convertMillisToDateFormat(long millis, String format) {
        if (format == null) format = "MM/dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        return sdf.format(new Date(millis));
    }

    /*public static Component getPlayerComponent(Player player, Component playerComponent, boolean prefix, boolean suffix) {
        Component baseComponent = Component.text("");
        if(prefix) baseComponent = baseComponent.append(Component.text(LuckPerms.getPrefix(player.getUniqueId())));
        if(playerComponent != null) {
            baseComponent = baseComponent.append(playerComponent.hoverEvent(HoverEvent.showEntity(HoverEvent.ShowEntity.of(Key.key("player"), player.getUniqueId(), Component.text(player.getName()))))
                    .clickEvent(ClickEvent.suggestCommand("/tell " + player.getName() + " ")));
        } else {
            baseComponent = baseComponent.append(player.displayName().hoverEvent(HoverEvent.showEntity(HoverEvent.ShowEntity.of(Key.key("player"), player.getUniqueId(), Component.text(player.getName()))))
                    .clickEvent(ClickEvent.suggestCommand("/tell " + player.getName() + " ")));
        }
        if(suffix) baseComponent = baseComponent.append(Component.text(LuckPerms.getSuffix(player.getUniqueId())));
        return baseComponent;
    }*/
}
