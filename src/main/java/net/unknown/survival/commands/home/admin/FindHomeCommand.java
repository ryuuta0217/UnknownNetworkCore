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

package net.unknown.survival.commands.home.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.survival.data.Home;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.enums.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class FindHomeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("findhome");
        builder.requires(Permissions.COMMAND_FINDHOME::checkAndIsPlayer);

        builder.then(Commands.argument("distance", DoubleArgumentType.doubleArg(1))
                .executes(FindHomeCommand::execute)
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(FindHomeCommand::execute)));

        dispatcher.register(builder);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        double distance = DoubleArgumentType.getDouble(ctx, "distance");
        Location loc = ctx.getSource().getBukkitLocation();

        Map<UUID, Map<String, List<Home>>> allHomes = new HashMap<>();
        PlayerData.getAll().forEach((uniqueId, data) -> {
            data.getGroups().forEach(groupName -> {
                Map<String, List<Home>> groupedHomes = allHomes.getOrDefault(uniqueId, new HashMap<>());
                List<Home> homes = groupedHomes.getOrDefault(groupName, new ArrayList<>());
                homes.addAll(data.getHomes(groupName).values());
                groupedHomes.put(groupName, homes);
                allHomes.put(uniqueId, groupedHomes);
            });
        });

        MessageUtil.sendAdminMessage(ctx.getSource(), distance + "ブロックの範囲に設定されているホームを検索しています...");

        AtomicBoolean searchAll = new AtomicBoolean(true);
        AtomicReference<Collection<ServerPlayer>> targets = new AtomicReference<>(null);
        if (BrigadierUtil.isArgumentKeyExists(ctx, "players")) {
            searchAll.set(false);
            targets.set(EntityArgument.getPlayers(ctx, "players"));
        }

        RunnableManager.runAsync(() -> {
            Map<UUID, Map<String, List<Home>>> resultsRaw = new HashMap<>();

            if (searchAll.get()) {
                allHomes.forEach((uniqueId, groupedHomes) -> {
                    groupedHomes.forEach((groupName, homes) -> {
                        homes.forEach(home -> {
                            searchHome(uniqueId, loc, distance, groupName, home, resultsRaw);
                        });
                    });
                });
            } else {
                targets.get().forEach(player -> {
                    PlayerData.of(player).getGroupedHomes().forEach((groupName, homes) -> {
                        homes.forEach((name, home) -> {
                            searchHome(player.getUUID(), loc, distance, groupName, home, resultsRaw);
                        });
                    });
                });
            }

            MessageUtil.sendAdminMessage(ctx.getSource(), resultsRaw.size() + "個のホームが見つかりました");

            StringBuilder sb = new StringBuilder("§r§d-§6=§d- §a見つかったホーム一覧§e(" + resultsRaw.size() + ") §d-§6=§d-§r");

            resultsRaw.forEach((uniqueId, groupedHomes) -> {
                groupedHomes.forEach((groupName, homes) -> {
                    homes.forEach(home -> {
                        sb.append("\n§7§o(").append((long) home.location().distance(loc)).append(" blks away)§r §6").append(Bukkit.getOfflinePlayer(uniqueId).getName()).append(": §b").append(groupName).append(":").append(home.name()).append(" §6-§r §a").append(home.location().getBlockX()).append("§6, §a").append(home.location().getBlockY()).append("§6, §a").append(home.location().getBlockZ()).append("§6");
                    });
                });
            });

            MessageUtil.sendAdminMessage(ctx.getSource(), sb.toString(), false);
        });
        return 0;
    }

    private static void searchHome(UUID uniqueId, Location centerLoc, double distance, String groupName, Home home, Map<UUID, Map<String, List<Home>>> output) {
        if (home.location().getWorld().getUID().equals(centerLoc.getWorld().getUID())) {
            double d = home.location().distance(centerLoc);
            if (d != -1 && d <= distance) {
                Map<String, List<Home>> groupedHomes = output.getOrDefault(uniqueId, new HashMap<>());
                List<Home> homes = groupedHomes.getOrDefault(groupName, new ArrayList<>());
                homes.add(home);
                groupedHomes.put(groupName, homes);
                output.put(uniqueId, groupedHomes);
            }
        }
    }
}
