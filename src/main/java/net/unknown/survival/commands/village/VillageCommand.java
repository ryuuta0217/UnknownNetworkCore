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

package net.unknown.survival.commands.village;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.Villages;
import net.unknown.survival.data.model.Village;
import net.unknown.survival.enums.Permissions;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class VillageCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("village");
        builder.then(Commands.argument("名前", StringArgumentType.string())
                        .requires(Permissions.COMMAND_VILLAGE::check)
                        .suggests((ctx, suggestionsBuilder) -> {
                            return SharedSuggestionProvider.suggest(
                                    Villages.getVillages()
                                            .values()
                                            .parallelStream()
                                            .map(Village::getName)
                                            .map(StringArgumentType::escapeIfRequired),
                                    suggestionsBuilder
                            );
                        })
                        .executes(VillageCommand::execTeleport)
                        .then(Commands.argument("対象", EntityArgument.entities())
                                .executes(VillageCommand::execTeleport)));
        dispatcher.register(builder);
    }

    private static int execTeleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> targets;
        if (BrigadierUtil.isArgumentKeyExists(ctx, "対象")) {
            targets = EntityArgument.getEntities(ctx, "対象");
        } else {
            targets = Set.of(ctx.getSource().getPlayerOrException());
        }

        String villageName = StringArgumentType.getString(ctx, "名前");
        Village village = Villages.getVillageByName(villageName);
        if (village == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "公認村 " + villageName + " は見つかりませんでした");
            return 0;
        }

        Location location = village.getLocation().asLocation();
        if (location == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "公認村 " + villageName + " の場所を特定できません");
            return 0;
        }
        ServerLevel level = MinecraftAdapter.level(location.getWorld());

        List<? extends Entity> successPlayers = targets.stream().filter(player -> player.teleportTo(level, location.getX(), location.getY(), location.getZ(), Collections.emptySet(), location.getYaw(), location.getPitch(), false, PlayerTeleportEvent.TeleportCause.COMMAND)).toList();
        boolean isAllPlayer = successPlayers.parallelStream().allMatch(entity -> entity instanceof ServerPlayer);
        if (successPlayers.size() > 1) NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text(successPlayers.size())).append(Component.text((isAllPlayer ? "人" : "のエンティティ") + "を")).appendSpace().append(village.getDisplayName()).appendSpace().append(Component.text("にテレポートさせました")));
        else NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(village.getDisplayName()).appendSpace().append(Component.text("にテレポートしました")));
        return targets.size();
    }
}
