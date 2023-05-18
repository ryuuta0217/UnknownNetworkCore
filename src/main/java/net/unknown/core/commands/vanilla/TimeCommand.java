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

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.kyori.adventure.sound.Sound;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.unknown.core.commands.Suggestions;
import net.unknown.core.commands.brigadier.CustomBrigadierExceptions;
import net.unknown.core.managers.RunnableManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Random;

public class TimeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("time");
        builder.requires(ctx -> ctx.hasPermission(2));

        for (TickName tick : TickName.values()) {
            builder.then(Commands.literal("set")
                    .then(Commands.literal(tick.getName())
                            .executes(ctx -> TimeCommand.setTime(ctx.getSource(), tick.getTicks(), null, false))
                            .then(Commands.argument("override", BoolArgumentType.bool())
                                    .executes(ctx -> TimeCommand.setTime(ctx.getSource(), tick.getTicks(), null, BoolArgumentType.getBool(ctx, "override"))))
                            .then(Commands.argument("world", StringArgumentType.string())
                                    .suggests(Suggestions.WORLD_SUGGEST)
                                    .executes(ctx -> TimeCommand.setTime(ctx.getSource(), tick.getTicks(), StringArgumentType.getString(ctx, "world"), false))
                                    .then(Commands.argument("override", BoolArgumentType.bool())
                                            .executes(ctx -> TimeCommand.setTime(ctx.getSource(), tick.getTicks(), StringArgumentType.getString(ctx, "world"), BoolArgumentType.getBool(ctx, "override"))))))
                    .then(Commands.argument("time", TimeArgument.time())
                            .executes(ctx -> TimeCommand.setTime(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "time"), null, false))
                            .then(Commands.argument("override", BoolArgumentType.bool())
                                    .executes(ctx -> TimeCommand.setTime(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "time"), null, BoolArgumentType.getBool(ctx, "override")))))
                            .then(Commands.argument("world", StringArgumentType.string())
                                    .suggests(Suggestions.WORLD_SUGGEST)
                                    .executes(ctx -> TimeCommand.setTime(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "time"), StringArgumentType.getString(ctx, "world"), false))
                                    .then(Commands.argument("override", BoolArgumentType.bool())
                                            .executes(ctx -> TimeCommand.setTime(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "time"), StringArgumentType.getString(ctx, "world"), BoolArgumentType.getBool(ctx, "override"))))));
        }

        builder.then(Commands.literal("add")
                        .then(Commands.argument("time", TimeArgument.time())
                                .executes(ctx -> TimeCommand.addTime(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "time"), null))
                                .then(Commands.argument("world", StringArgumentType.string())
                                        .suggests(Suggestions.WORLD_SUGGEST)
                                        .executes(ctx -> TimeCommand.addTime(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "time"), StringArgumentType.getString(ctx, "world"))))));

        for (QueryType query : QueryType.values()) {
            builder.then(Commands.literal("query")
                    .then(Commands.literal(query.name().toLowerCase())
                            .executes(ctx -> TimeCommand.queryTime(ctx.getSource(), null, query))
                            .then(Commands.argument("world", StringArgumentType.string())
                                    .suggests(Suggestions.WORLD_SUGGEST)
                                    .executes(ctx -> TimeCommand.queryTime(ctx.getSource(), StringArgumentType.getString(ctx, "world"), query)))));
        }

        dispatcher.register(builder);
    }

    private static int queryTime(CommandSourceStack source, String worldName, QueryType type) throws CommandSyntaxException {
        if (worldName != null && Bukkit.getWorld(worldName) == null) {
            throw CustomBrigadierExceptions.UNKNOWN_DIMENSION.create(worldName);
        }

        ServerLevel level = worldName == null ? source.getLevel() : ((CraftWorld) Bukkit.getWorld(worldName)).getHandle();
        int time;
        switch (type) {
            case DAY -> time = getDay(level);
            case DAYTIME -> time = getDayTime(level);
            case GAMETIME -> time = getGameTime(level);
            default -> throw new IllegalStateException("Failed to parse");
        }

        source.sendSuccess(Component.translatable("commands.time.query", time), false);
        return time;
    }

    public static int setTime(CommandSourceStack source, int time, String worldName, boolean override) throws CommandSyntaxException {
        ServerLevel targetLevel;
        if (worldName != null && Bukkit.getWorld(worldName) == null) {
            throw CustomBrigadierExceptions.UNKNOWN_DIMENSION.create(worldName);
        }

        if (worldName != null && Bukkit.getWorld(worldName) != null) targetLevel = ((CraftWorld) Bukkit.getWorld(worldName)).getHandle();
        else targetLevel = source.getLevel();
        if (!override) {
            throw new SimpleCommandExceptionType(Component.literal("""
                    /time set コマンドの実行は危険な副作用を伴います。
                    通常は使用しないことを推奨します。
                    /time add ... の使用を検討してください。

                    この警告を無視してもなお実行する場合は、overwrite引数をtrueにして、再試行してください。
                    発生する副作用は重大なものです。使用する場合は、二重のチェックをお忘れなく。""").withStyle(ChatFormatting.BOLD)).create();
        }

        Iterator<ServerLevel> iterator = worldName == null && io.papermc.paper.configuration.GlobalConfiguration.get().commands.timeCommandAffectsAllWorlds ? source.getServer().getAllLevels().iterator() : com.google.common.collect.Iterators.singletonIterator(targetLevel); // CraftBukkit - SPIGOT-6496: Only set the time for the world the command originates in // Paper - add config option for spigot's change

        while (iterator.hasNext()) {
            ServerLevel worldserver = iterator.next();

            // CraftBukkit start
            TimeSkipEvent event = new TimeSkipEvent(worldserver.getWorld(), TimeSkipEvent.SkipReason.COMMAND, time - worldserver.getDayTime());
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                worldserver.setDayTime(worldserver.getDayTime() + event.getSkipAmount());
            }
            // CraftBukkit end
        }

        source.sendSuccess(Component.translatable("commands.time.set", time), true);
        return TimeCommand.getDayTime(source.getLevel());
    }

    public static int addTime(CommandSourceStack source, int time, String worldName) throws CommandSyntaxException {
        ServerLevel targetLevel;
        if (worldName != null && Bukkit.getWorld(worldName) == null) {
            throw CustomBrigadierExceptions.UNKNOWN_DIMENSION.create(worldName);
        }

        if (worldName != null && Bukkit.getWorld(worldName) != null) targetLevel = ((CraftWorld) Bukkit.getWorld(worldName)).getHandle();
        else targetLevel = source.getLevel();

        Iterator<ServerLevel> iterator = io.papermc.paper.configuration.GlobalConfiguration.get().commands.timeCommandAffectsAllWorlds ? source.getServer().getAllLevels().iterator() : com.google.common.collect.Iterators.singletonIterator(targetLevel); // CraftBukkit - SPIGOT-6496: Only set the time for the world the command originates in // Paper - add config option for spigot's change

        while (iterator.hasNext()) {
            ServerLevel worldserver = iterator.next();

            // CraftBukkit start
            TimeSkipEvent event = new TimeSkipEvent(worldserver.getWorld(), TimeSkipEvent.SkipReason.COMMAND, time);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                worldserver.setDayTime(worldserver.getDayTime() + event.getSkipAmount());
            }
            // CraftBukkit end
        }

        int j = TimeCommand.getDayTime(source.getLevel());

        source.sendSuccess(Component.translatable("commands.time.set", j), true);
        return j;
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
        MORNING(0),
        DAY(1000),
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
