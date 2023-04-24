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

package net.unknown.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.enums.Permissions;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class SkullCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("skull");
        builder.requires(Permissions.COMMAND_SKULL::checkAndIsPlayer);
        builder.then(Commands.argument("名前", StringArgumentType.word())
                .suggests(Suggestions.ALL_PLAYER_SUGGEST)
                .executes(SkullCommand::execute)
                .then(Commands.argument("個数", IntegerArgumentType.integer(1, 64))
                        .executes(SkullCommand::execute)));
        dispatcher.register(builder);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "名前");
        int count = BrigadierUtil.getArgumentOrDefault(ctx, int.class, "個数", 1);

        UUID uniqueId = Bukkit.getPlayerUniqueId(id);
        if (uniqueId == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.text("プレイヤー " + id + " は見つかりませんでした"));
            return 1;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(uniqueId);

        ItemStack playerHead = new ItemStackBuilder(Material.PLAYER_HEAD)
                .custom(is -> {
                    is.setAmount(count);
                    SkullMeta meta = (SkullMeta) is.getItemMeta();
                    meta.setOwningPlayer(player);
                    is.setItemMeta(meta);
                }).build();


        Inventory inv = ctx.getSource().getPlayerOrException().getBukkitEntity().getInventory();
        if (inv.firstEmpty() != -1) {
            inv.addItem(playerHead);
            NewMessageUtil.sendMessage(ctx.getSource(), Component.text(id + " の頭を" + (count != 1 ? count + "個" : "") + "入手しました"));
            return 0;
        } else {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.text("インベントリがいっぱいです"));
            return 2;
        }
    }
}
