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
import net.md_5.bungee.api.ChatColor;
import net.minecraft.commands.CommandSourceStack;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.survival.data.Home;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.enums.Permissions;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import net.minecraft.commands.Commands;

import java.util.List;
import java.util.Set;
import java.util.UUID;

// /minecraft:homes [optional: int<page>] - send homes list
public class HomesCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("homes");
        builder.requires(Permissions.COMMAND_HOMES::checkAndIsPlayer);
        builder.executes(HomesCommand::sendHomeList);
        builder.then(Commands.argument("ページ", IntegerArgumentType.integer(1)).executes(HomesCommand::sendHomeList));
        dispatcher.register(builder);
    }

    public static int sendHomeList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CraftPlayer player = (CraftPlayer) ctx.getSource().getBukkitEntity();
        PlayerData data = PlayerData.of(player);

        StringBuilder sb = new StringBuilder(MessageUtil.getMessagePrefix() + "§d-§6=§d- §aカテゴリ uncategorized のホーム一覧(" + data.getHomes("uncategorized").size() + getMaxCountStr(player.getUniqueId()) + ") §d-§6=§d-");

        int page = BrigadierUtil.getArgumentOrDefault(ctx, Integer.class, "ページ", 1);
        int internalPage = page-1;

        List<Set<Home>> pagination = ListUtil.splitListAsSet(data.getHomes("uncategorized").values(), 10);

        int maxPage = pagination.size();
        if(page > maxPage) {
            MessageUtil.sendErrorMessage(ctx.getSource(), "最大ページ数は " + maxPage + " です");
            return 1;
        }

        Set<Home> homes = pagination.get(internalPage);

        homes.forEach((home) -> {
            Location loc = home.location();
            sb.append("\n").append(MessageUtil.getMessagePrefix()).append(String.format("§b%s §6-§r §e%s§6, §a%s§6, §a%s§6, §a%s§6, §d%s§6, §d%s§r", home.name(), MessageUtil.getWorldNameDisplay(loc.getWorld()), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getYaw(), loc.getPitch()));
        });

        Component c = Component.text(sb.toString())
                .append(Component.text("\n" + MessageUtil.getMessagePrefix() + "                        "));

        c = c.append(Component.text("⇦", TextColor.color(ChatColor.RED.getColor().getRGB()))
                        .clickEvent(page > 1 ? ClickEvent.runCommand("/homes " + (page-1)) : null))
              .append(Component.text(" [" + page + "/" + maxPage + "] ",
                      TextColor.color(ChatColor.AQUA.getColor().getRGB())));

        if(page < maxPage) {
            c = c.append(Component.text("⇨", TextColor.color(ChatColor.GREEN.getColor().getRGB()))
                    .clickEvent(ClickEvent.runCommand("/homes " + (page+1))));
        }

        ctx.getSource().getPlayerOrException().getBukkitEntity().sendMessage(c);
        return 1;
    }

    public static String getMaxCountStr(UUID uniqueId) {
        int maxHomeCount = PlayerData.of(uniqueId).getMaxHomeCount();
        if (maxHomeCount == -1) return "";
        return "/" + maxHomeCount;
    }
}
