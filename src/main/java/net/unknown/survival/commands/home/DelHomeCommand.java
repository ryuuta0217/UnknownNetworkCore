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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.unknown.core.util.MessageUtil;
import net.unknown.survival.commands.Suggestions;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.data.model.HomeGroup;
import net.unknown.survival.enums.Permissions;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;

// /minecraft:delhome <String: ホーム名> - remove home
public class DelHomeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("delhome");
        builder.requires(Permissions.COMMAND_DELHOME::checkAndIsPlayer);
        builder.then(Commands.argument("ホーム名", StringArgumentType.greedyString())
                .suggests(Suggestions.HOME_SUGGEST)
                .executes(ctx -> {
                    if (ctx.getSource().getBukkitEntity() instanceof CraftPlayer) {
                        String homeName = StringArgumentType.getString(ctx, "ホーム名");
                        CraftPlayer player = (CraftPlayer) ctx.getSource().getBukkitEntity();
                        PlayerData.HomeData data = PlayerData.of(player).getHomeData();
                        HomeGroup defaultGroup = data.getDefaultGroup();
                        if (defaultGroup.hasHome(homeName)) {
                            defaultGroup.removeHome(homeName);
                            MessageUtil.sendMessage(ctx.getSource(), "ホーム " + homeName + " を§c§l削除§rしました");
                        } else {
                            MessageUtil.sendErrorMessage(ctx.getSource(), "ホーム " + homeName + " は存在しません");
                        }
                    } else {
                        MessageUtil.sendErrorMessage(ctx.getSource(), "ゲーム内から実行してください");
                    }
                    return 1;
                }));
        dispatcher.register(builder);
    }
}
