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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.enums.Permissions;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.dependency.MultiverseCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TeleportWorldCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("teleportworld");
        builder.requires(Permissions.COMMAND_TELEPORTWORLD::check);

        builder.then(Commands.argument("ワールド名", StringArgumentType.word())
                .suggests(Suggestions.WORLD_SUGGEST)
                .executes(TeleportWorldCommand::execute)
                .then(Commands.argument("対象", EntityArgument.entities())
                        .executes(TeleportWorldCommand::execute))
                .then(Commands.argument("座標", Vec3Argument.vec3())
                        .executes(TeleportWorldCommand::execute)
                        .then(Commands.argument("対象", EntityArgument.entities())
                                .executes(TeleportWorldCommand::execute))
                        .then(Commands.argument("向き", RotationArgument.rotation())
                                .executes(TeleportWorldCommand::execute)
                                .then(Commands.argument("対象", EntityArgument.entities())
                                        .executes(TeleportWorldCommand::execute)))));

        LiteralArgumentBuilder<CommandSourceStack> aliasBuilder = LiteralArgumentBuilder.literal("tpw");
        aliasBuilder.requires(Permissions.COMMAND_TELEPORTWORLD::check);
        aliasBuilder.redirect(dispatcher.register(builder));
        dispatcher.register(aliasBuilder);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String levelName = StringArgumentType.getString(ctx, "ワールド名");
        World bukkitWorld = Bukkit.getWorld(levelName);
        if (bukkitWorld == null) {
            MessageUtil.sendErrorMessage(ctx.getSource(), "ワールド " + levelName + " は見つかりませんでした");
            return -1;
        }
        Location spawnLoc = MultiverseCore.getSpawnLocation(bukkitWorld);
        Vec3 position = BrigadierUtil.isArgumentKeyExists(ctx, "座標") ? Vec3Argument.getVec3(ctx, "座標") : new Vec3(spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ());
        Vec2 rotation = BrigadierUtil.isArgumentKeyExists(ctx, "向き") ? RotationArgument.getRotation(ctx, "向き").getRotation(ctx.getSource()) : new Vec2(spawnLoc.getYaw(), spawnLoc.getPitch());

        Location toLoc = new Location(bukkitWorld, position.x(), position.y(), position.z(), rotation.x, rotation.y);

        List<Entity> targets = BrigadierUtil.isArgumentKeyExists(ctx, "対象") ? EntityArgument.getEntities(ctx, "対象").stream().map(e -> (Entity) e.getBukkitEntity()).toList() : Collections.singletonList(ctx.getSource().getBukkitEntity());
        if (ctx.getSource().source instanceof BaseCommandBlock && targets.size() == 1 && targets.stream().anyMatch(Objects::isNull)) {
            MessageUtil.sendErrorMessage(ctx.getSource(), "セレクタが必要です");
            return -2;
        }

        targets = targets.stream().filter(Objects::nonNull).toList();

        targets.forEach(e -> e.teleportAsync(toLoc, PlayerTeleportEvent.TeleportCause.COMMAND));
        String message = "";

        if (targets.size() > 1) message += targets.size() + "体のエンティティを";
        else if (!targets.get(0).equals(ctx.getSource().getBukkitEntity())) message += targets.get(0).getName() + "を";

        message += MessageUtil.getWorldName(levelName) + "(" + levelName + ")にテレポート";

        if (targets.size() > 1 || !targets.get(0).equals(ctx.getSource().getBukkitEntity())) {
            message += "させました";
        } else {
            message += "しました";
        }

        MessageUtil.sendMessage(ctx.getSource(), message);
        return 0;
    }
}
