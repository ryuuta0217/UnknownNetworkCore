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

package net.unknown.survival.commands.home.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.unknown.core.util.MessageUtil;
import net.unknown.survival.commands.BrigadierCommandSuggestions;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.enums.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import net.minecraft.commands.Commands;

import java.util.UUID;

public class ADelHomeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("adelhome");
        builder.requires(Permissions.COMMAND_ADELHOME::check);

        builder.then(Commands.argument("対象", StringArgumentType.word())
                .suggests(BrigadierCommandSuggestions.ALL_PLAYER_SUGGEST)
                .executes(AHomesCommand::sendHomeList)
                .then(Commands.argument("ホーム名", StringArgumentType.greedyString())
                        .suggests(BrigadierCommandSuggestions.OFFLINE_HOME_SUGGEST)
                        .executes(ADelHomeCommand::execute)));

        dispatcher.register(builder);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        String homeName = StringArgumentType.getString(ctx, "ホーム名");
        String inputPlayerId = StringArgumentType.getString(ctx, "対象");
        UUID homeOwnerUniqueId = Bukkit.getPlayerUniqueId(inputPlayerId);
        PlayerData homeOwnerData = PlayerData.of(homeOwnerUniqueId);

        if (homeOwnerUniqueId == null) {
            MessageUtil.sendAdminErrorMessage(ctx.getSource(), "プレイヤー " + inputPlayerId + " は存在しません");
            return 1;
        }

        OfflinePlayer homeOwner = Bukkit.getOfflinePlayer(homeOwnerUniqueId);

        if (homeOwnerData.isHomeExists("uncategorized", homeName)) {
            homeOwnerData.removeHome("uncategorized", homeName);
            if (!homeOwnerData.isHomeExists("uncategorized", homeName)) {
                MessageUtil.sendAdminMessage(ctx.getSource(), homeOwner.getName() + " のホーム " + homeName + " を§c§l削除§rしました");
            } else {
                MessageUtil.sendAdminErrorMessage(ctx.getSource(), homeOwner.getName() + " のホーム " + homeName + " を削除できませんでした");
            }
        } else {
            MessageUtil.sendAdminErrorMessage(ctx.getSource(), homeOwner.getName() + " のホーム " + homeName + " は存在しません。");
        }
        return 0;
    }
}
