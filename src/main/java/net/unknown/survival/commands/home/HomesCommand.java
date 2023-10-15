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

package net.unknown.survival.commands.home;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ryuuta0217.util.ListUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.core.define.DefinedComponents;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.model.Home;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.data.model.HomeGroup;
import net.unknown.survival.enums.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.*;

// /minecraft:homes [optional: int<page>] - send homes list
public class HomesCommand {
    private static final Component PREFIX = Component.empty()
            .append(Component.text("-", DefinedTextColor.LIGHT_PURPLE))
            .append(Component.text("=", DefinedTextColor.GOLD))
            .append(Component.text("-", DefinedTextColor.LIGHT_PURPLE));

    private static final Component DELIMITER = Component.text(", ");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("homes");
        builder.requires(Permissions.COMMAND_HOMES::checkAndIsPlayer);
        builder.executes(HomesCommand::sendHomeList);
        builder.then(Commands.argument("ページ", IntegerArgumentType.integer(1)).executes(HomesCommand::sendHomeList));
        dispatcher.register(builder);
    }

    public static int sendHomeList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return sendHomeList(ctx, ctx.getSource().getPlayerOrException().getUUID());
    }

    public static int sendHomeList(CommandContext<CommandSourceStack> ctx, UUID target) throws CommandSyntaxException {
        UUID executor = ctx.getSource().source instanceof ServerPlayer sourcePlayer ? sourcePlayer.getUUID() : Util.NIL_UUID;

        PlayerData data = PlayerData.of(target);
        PlayerData.HomeData homeData = data.getHomeData();
        HomeGroup homeGroup = homeData.getDefaultGroup();

        Component header = Component.empty()
                .append(PREFIX)
                .append(DefinedComponents.SPACE)
                .append(Component.text((Objects.equals(executor, target) ? "" : Bukkit.getOfflinePlayer(target).getName() + "の") + "グループ " + homeGroup.getName() + " のホーム一覧(" + homeGroup.getHomes().size() + "/" + homeData.getMaxHomeCount() + ")", DefinedTextColor.GREEN))
                .append(DefinedComponents.SPACE)
                .append(PREFIX);

        int page = BrigadierUtil.getArgumentOrDefault(ctx, Integer.class, "ページ", 1);
        int internalPage = page - 1;

        List<Set<Home>> pagination = ListUtil.splitListAsSet(homeGroup.getHomes().values(), 10);

        int maxPage = pagination.size();
        if (page > maxPage) {
            MessageUtil.sendErrorMessage(ctx.getSource(), "最大ページ数は " + maxPage + " です");
            return 1;
        }

        Set<Home> toShowHomes = pagination.get(internalPage);
        List<Component> contents = new ArrayList<>();

        toShowHomes.forEach((home) -> {
            Component element = Component.empty().decoration(TextDecoration.STRIKETHROUGH, !home.isAvailable())
                    .append(Component.text(home.name(), DefinedTextColor.AQUA))
                    .append(DefinedComponents.SPACE)
                    .append(Component.text("-", DefinedTextColor.GOLD))
                    .append(DefinedComponents.SPACE)
                    .append(Component.text(MessageUtil.getWorldName(home.worldName())))
                    .append(DELIMITER)
                    .append(Component.text(home.x(), DefinedTextColor.GREEN))
                    .append(DELIMITER)
                    .append(Component.text(home.y(), DefinedTextColor.GREEN))
                    .append(DELIMITER)
                    .append(Component.text(home.z(), DefinedTextColor.GREEN))
                    .append(DELIMITER)
                    .append(Component.text(home.yaw(), DefinedTextColor.LIGHT_PURPLE))
                    .append(DELIMITER)
                    .append(Component.text(home.pitch(), DefinedTextColor.LIGHT_PURPLE));
            contents.add(element);
        });

        Component toShowMessage = Component.empty()
                .append(header);

        for (Component content : contents) {
            toShowMessage = toShowMessage.append(DefinedComponents.NEW_LINE)
                    .append(content);
        }

        Component footer = Component.empty()
                .append(Component.text("                 "));

        if (page > 1) {
            footer = footer.append(Component.text("⇦", DefinedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/homes " + (page - 1))));
        } else {
            footer = footer.append(Component.text(" "));
        }

        footer = footer.append(DefinedComponents.SPACE)
                .append(Component.text("[" + page + "/" + maxPage + "]", DefinedTextColor.AQUA))
                .append(DefinedComponents.SPACE);

        if (page < maxPage) {
            footer = footer.append(Component.text("⇨", DefinedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/homes " + (page + 1))));
        }

        toShowMessage = toShowMessage.append(DefinedComponents.NEW_LINE)
                .append(footer);

        NewMessageUtil.sendMessage(ctx.getSource(), toShowMessage, false);
        //ctx.getSource().getPlayerOrException().getBukkitEntity().sendMessage(toShowMessage);
        return homeGroup.getHomes().size();
    }
}
