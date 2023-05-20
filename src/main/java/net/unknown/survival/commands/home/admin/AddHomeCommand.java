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

package net.unknown.survival.commands.home.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.commands.Suggestions;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.data.model.Home;
import net.unknown.survival.data.model.HomeGroup;
import net.unknown.survival.enums.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class AddHomeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("addhome");
        builder.requires(Permissions.COMMAND_ADDHOME::checkAndIsPlayer);

        // /addhome <player> <homeName> <loc:x,y,z> <rot:yaw,pitch> [world]
        builder.then(Commands.argument("target", StringArgumentType.word())
                .suggests(Suggestions.ALL_PLAYER_SUGGEST)
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(AddHomeCommand::execute) // /addhome <player> <homeName>
                        .then(Commands.argument("overwrite", BoolArgumentType.bool())
                                .executes(AddHomeCommand::execute)) // /addhome <player> <homeName> <overwrite>
                        .requires(Permissions.COMMAND_ADDHOME::check)
                        .then(Commands.argument("location", Vec3Argument.vec3())
                                .executes(AddHomeCommand::execute) // /addhome <player> <homeName> <x> <y> <z>
                                .then(Commands.argument("overwrite", BoolArgumentType.bool())
                                        .executes(AddHomeCommand::execute)) // /addhome <player> <homeName> <x> <y> <z> <overwrite>
                                .then(Commands.argument("rotation", RotationArgument.rotation())
                                        .executes(AddHomeCommand::execute) // /addhome <player> <homeName> <x> <y> <z> <yaw> <pitch>
                                        .then(Commands.argument("overwrite", BoolArgumentType.bool())
                                                .executes(AddHomeCommand::execute)) // /addhome <player> <homeName> <x> <y> <z> <yaw> <pitch> <overwrite>
                                        .then(Commands.argument("world", StringArgumentType.string())
                                                .suggests(Suggestions.WORLD_SUGGEST)
                                                .executes(AddHomeCommand::execute) // /addhome <player> <homeName> <x> <y> <z> <yaw> <pitch> <world>
                                                .then(Commands.argument("overwrite", BoolArgumentType.bool())
                                                        .executes(AddHomeCommand::execute))))))); // /addhome <player> <homeName> <x> <y> <z> <yaw> <pitch> <world> <overwrite>

        dispatcher.register(builder);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String targetName = StringArgumentType.getString(ctx, "target");
        String name = StringArgumentType.getString(ctx, "name");
        UUID targetUniqueId = Bukkit.getPlayerUniqueId(targetName);
        PlayerData targetData = PlayerData.of(targetUniqueId);
        PlayerData.HomeData targetHomeData = targetData.getHomeData();
        HomeGroup defaultGroup = targetHomeData.getDefaultGroup();

        if (targetUniqueId == null) {
            MessageUtil.sendAdminErrorMessage(ctx.getSource(), "プレイヤー " + targetName + " は見つかりませんでした");
            return 1;
        }

        boolean overwrite = BrigadierUtil.isArgumentKeyExists(ctx, "overwrite") && BoolArgumentType.getBool(ctx, "overwrite");

        if (defaultGroup.hasHome(name) && !overwrite) {
            MessageUtil.sendAdminErrorMessage(ctx.getSource(), "プレイヤー " + targetName + " はグループ " + defaultGroup.getName() + " にホーム " + name + " を既に設定しています");
            return 2;
        }

        Vec3 location = BrigadierUtil.isArgumentKeyExists(ctx, "location") ? Vec3Argument.getVec3(ctx, "location") : ctx.getSource().getPosition();
        Vec2 rotation = BrigadierUtil.isArgumentKeyExists(ctx, "rotation") ? RotationArgument.getRotation(ctx, "rotation").getRotation(ctx.getSource()) : ctx.getSource().getRotation();
        String worldName = BrigadierUtil.isArgumentKeyExists(ctx, "world") ? StringArgumentType.getString(ctx, "world") : ctx.getSource().getBukkitWorld().getName();
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            MessageUtil.sendAdminErrorMessage(ctx.getSource(), "ワールド " + worldName + " は見つかりませんでした");
            return 3;
        }

        Location loc = MinecraftAdapter.location(MinecraftAdapter.level(world), location, rotation);
        defaultGroup.addHome(new Home(name, loc), overwrite);
        MessageUtil.sendAdminMessage(ctx.getSource(), "プレイヤー " + targetName + " のグループ " + defaultGroup.getName() + " にホーム " + name + " を設定しました");
        return 0;
    }
}
