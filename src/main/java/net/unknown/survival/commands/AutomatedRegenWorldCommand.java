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

package net.unknown.survival.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.survival.world.regen.AutomatedRegenWorldManager;

import java.time.*;
import java.time.format.DateTimeFormatter;

// /<automatedregenworld|arw> <add|remove|list|reload>
public class AutomatedRegenWorldCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("automatedregenworld");

        builder.then(Commands.literal("add")
                .then(Commands.argument("date", StringArgumentType.string())
                        .suggests((ctx, suggestionsBuilder) -> suggestionsBuilder.suggest("\"" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")) + "\"").buildFuture())
                        .then(Commands.argument("worlds", StringArgumentType.string())
                                .then(Commands.argument("seed", StringArgumentType.string())
                                        .then(Commands.argument("keep-game-rule", BoolArgumentType.bool())
                                                .then(Commands.argument("pre-generate", BoolArgumentType.bool())
                                                        .then(Commands.argument("keep-old-world", BoolArgumentType.bool())
                                                                .executes(AutomatedRegenWorldCommand::addTask))))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("date", LongArgumentType.longArg(System.currentTimeMillis()))
                                .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggest(AutomatedRegenWorldManager.getInstance().getTasks().keySet().stream().map(String::valueOf), suggestionsBuilder))
                                .executes(AutomatedRegenWorldCommand::removeTask)))
                .then(Commands.literal("list")
                        .executes(AutomatedRegenWorldCommand::listTasks))
                .then(Commands.literal("reload")
                        .executes(AutomatedRegenWorldCommand::reloadTasks));

        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("arw").redirect(dispatcher.register(builder)));
    }

    public static int addTask(CommandContext<CommandSourceStack> ctx) {
        String dateStr = StringArgumentType.getString(ctx, "date");
        String[] worlds = StringArgumentType.getString(ctx, "worlds").split(",");
        String seed = BrigadierUtil.getArgumentOrDefault(ctx, String.class, "seed", null);
        if (seed.equals("null")) seed = null; // Handle "null" as a string))
        boolean keepGameRule = BoolArgumentType.getBool(ctx, "keep-game-rule");
        boolean preGenerate = BoolArgumentType.getBool(ctx, "pre-generate");
        boolean keepOldWorld = BoolArgumentType.getBool(ctx, "keep-old-world");

        LocalDateTime dateTime;
        try {
            String[] dateAndTime = dateStr.split("([ 　])");
            if (dateAndTime.length == 2) {
                LocalDate date;
                String[] dateParts = dateAndTime[0].split("/", 3);
                if (dateParts.length == 3 && dateParts[0].matches("\\d{4}") && dateParts[1].matches("\\d{1,2}") && dateParts[2].matches("\\d{1,2}")) {
                    date = LocalDate.of(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[2]));
                } else {
                    ctx.getSource().sendFailure(Component.literal("日付の形式が正しくありません。例: 2023/10/01"));
                    return -1;
                }

                String[] timeParts = dateAndTime[1].split(":", 3);
                LocalTime time = LocalTime.of(timeParts[0].matches("\\d{1,2}") ? Integer.parseInt(timeParts[0]) : 0,
                        timeParts.length > 1 && timeParts[1].matches("\\d{1,2}") ? Integer.parseInt(timeParts[1]) : 0,
                        timeParts.length > 2 && timeParts[2].matches("\\d{1,2}") ? Integer.parseInt(timeParts[2]) : 0);

                dateTime = LocalDateTime.of(date, time);
            } else {
                ctx.getSource().sendFailure(Component.literal("日付と時間をスペースで区切って入力してください。例: 2023/10/01 12:00"));
                return -1;
            }
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("日付の形式が正しくありません。例: 2023/10/01 12:00"));
            return 0;
        }

        long date = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        AutomatedRegenWorldManager.getInstance().addTask(date, worlds, seed, keepGameRule, preGenerate, keepOldWorld);
        ctx.getSource().sendSuccess(() -> Component.literal("追加しました: " + date + " (" + dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")) + ")"), true);
        return 1;
    }

    public static int removeTask(CommandContext<CommandSourceStack> ctx) {
        long date = LongArgumentType.getLong(ctx, "date");
        AutomatedRegenWorldManager.getInstance().removeTask(date);
        ctx.getSource().sendSuccess(() -> Component.literal("削除しました: " + date), true);
        return 1;
    }

    public static int listTasks(CommandContext<CommandSourceStack> ctx) {
        AutomatedRegenWorldManager.getInstance().getTasks().forEach((date, task) -> {
            ctx.getSource().sendSuccess(() -> Component.literal("実行日時: " + date + "(" + Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")) + "), タスク: " + task), false);
        });
        return 1;
    }

    public static int reloadTasks(CommandContext<CommandSourceStack> ctx) {
        AutomatedRegenWorldManager.getInstance().reload();
        ctx.getSource().sendSuccess(() -> Component.literal("再読み込みしました。"), true);
        return 1;
    }
}
