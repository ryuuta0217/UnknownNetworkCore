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

package net.unknown.core.commands.vanilla;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.unknown.core.commands.Suggestions;
import net.unknown.core.enums.Permissions;
import net.unknown.core.util.BrigadierUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;

public class TimeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        BrigadierUtil.forceUnregisterCommand("time");

        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("time");
        builder.requires(Permissions.COMMAND_TIME::check);

        for (TickName tickName : TickName.values()) {
            builder.then(Commands.literal("set")
                    // /time set <tickName> [world]
                    // world を省略した場合、 execute in ... で指定された ServerLevel またはデフォルトの ServerLevel に対して実行される
                    .then(Commands.literal(tickName.getName())
                            .executes(ctx -> setTime(ctx, ctx.getSource().getLevel(), tickName.getTicks()))
                            .then(Commands.argument("world", StringArgumentType.string())
                                    .suggests(Suggestions.WORLD_SUGGEST)
                                    .executes(ctx -> setTimeAsWorldName(ctx, StringArgumentType.getString(ctx, "world"), tickName.getTicks()))))
                    .then(Commands.argument("time", TimeArgument.time())
                            .executes(ctx -> setTime(ctx, ctx.getSource().getLevel(), IntegerArgumentType.getInteger(ctx, "time")))
                            .then(Commands.argument("world", StringArgumentType.string())
                                    .suggests(Suggestions.WORLD_SUGGEST)
                                    .executes(ctx -> addTimeAsWorldName(ctx, StringArgumentType.getString(ctx, "world"), IntegerArgumentType.getInteger(ctx, "time"))))));
        }

        dispatcher.register(builder);
    }

    private static int setTime(CommandContext<CommandSourceStack> ctx, ServerLevel targetLevel, int newDayTimeTicks) {
        targetLevel.setDayTime(newDayTimeTicks);
        ctx.getSource().sendSuccess(new TextComponent("ワールド " + targetLevel.serverLevelData.getLevelName() + " の")
                .append(new TranslatableComponent("commands.time.set", newDayTimeTicks)), true);
        return getDayTime(targetLevel);
    }

    private static int setTimeAsWorldName(CommandContext<CommandSourceStack> ctx, String worldName, int newDayTimeTicks) {
        World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld != null) {
            return setTime(ctx, ((CraftWorld) bukkitWorld).getHandle(), newDayTimeTicks);
        } else {
            ctx.getSource().sendFailure(new TextComponent("ワールド " + worldName + " は見つかりませんでした"));
            return -1;
        }
    }

    private static int addTime(CommandContext<CommandSourceStack> ctx, ServerLevel targetLevel, int additionalDayTimeTicks) {
        return setTime(ctx, targetLevel, getDayTime(targetLevel) + additionalDayTimeTicks);
    }

    private static int addTimeAsWorldName(CommandContext<CommandSourceStack> ctx, String worldName, int additionalDayTimeTicks) {
        World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld != null) {
            return addTime(ctx, ((CraftWorld) bukkitWorld).getHandle(), additionalDayTimeTicks);
        } else {
            ctx.getSource().sendFailure(new TextComponent("ワールド " + worldName + " は見つかりませんでした"));
            return -1;
        }
    }

    private static int getDay(ServerLevel level) {
        return (int) (level.getDayTime() / 24000L % 2147483647L);
    }

    private static int getDayTime(ServerLevel level) {
        return (int) (level.getDayTime() % 24000L);
    }

    private static int getGameTime(ServerLevel level) {
        return (int) (level.getGameTime() % 2147483647L);
    }

    private enum TickName {
        SUNRISE(23000),
        DAY(0),
        MORNING(1000),
        NOON(6000),
        AFTERNOON(9000),
        SUNSET(12000),
        NIGHT(14000),
        MIDNIGHT(18000);

        private final int ticks;

        TickName(int ticks) {
            this.ticks = ticks;
        }

        public String getName() {
            return this.name().toLowerCase();
        }

        public int getTicks() {
            return ticks;
        }
    }

    private enum QueryType {
        DAY,
        DAYTIME,
        GAMETIME
    }
}
