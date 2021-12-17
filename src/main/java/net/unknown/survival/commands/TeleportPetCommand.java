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

package net.unknown.survival.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.survival.enums.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import net.minecraft.commands.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TeleportPetCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("tppet");
        builder.requires(Permissions.COMMAND_TELEPORT_PET::checkAndIsPlayer);

        builder.then(Commands.argument("番号", IntegerArgumentType.integer(0))
                .executes(TeleportPetCommand::execute));

        dispatcher.register(builder);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<Entity> tamedEntities = new ArrayList<>(); // 手懐けているエンティティを格納するList
        Bukkit.getWorlds().forEach(w -> { //全ワールドで
            tamedEntities.addAll(w.getEntities().stream() // ワールドにいる読み込まれているすべてのエンティティ
                    .filter(e -> e instanceof Tameable && // 手懐けられるエンティティだけに限定
                    ((Tameable) e).getOwnerUniqueId() != null && // 誰かに手懐けられていて
                    ((Tameable) e).getOwnerUniqueId().equals(ctx.getSource().getBukkitEntity().getUniqueId())) // 手懐けているのが実行者ならok(tamedEntitiesに追加される)
                    .collect(Collectors.toList()));
        });
        if(tamedEntities.size() == 0) return -1; // 手懐けているエンティティがひとつもみつからなかったらここまで

        // 数字が指定されていたらその数字を使用、指定されていなかったら0を使用
        int num = BrigadierUtil.getArgumentOrDefault(ctx, Integer.class, "番号", 0);
        if(num > tamedEntities.size()-1) num = tamedEntities.size()-1; // 手懐けているエンティティの数を数字が上回っていたら最大値にする
        Entity e = tamedEntities.get(num); // TP対象のエンティティを取得する
        ctx.getSource().getPlayerOrException().getBukkitEntity().teleport(e); // 対象にテレポートするおはよう
        // ohayou
        // 今日何日？tyやっときた
        // さっき言ったメッセージのやつよろしくたのむhome doko仕事してくるるくてazasu
        // 下の方 net.unknown.survival.commands.home.HomeCommand この順でたどって
        //ﾌﾝ23bkns
        //これだけだと味気ないな
        MessageUtil.sendMessage(ctx.getSource(),"番号"+ num + "のペットにテレポートしました");
        return 0;
    }
}
