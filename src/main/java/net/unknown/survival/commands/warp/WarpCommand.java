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

package net.unknown.survival.commands.warp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.Warps;
import net.unknown.survival.data.model.Warp;
import org.bukkit.Material;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;

import java.util.Collection;
import java.util.List;

public class WarpCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("warp");
        builder.then(Commands.argument("名前", StringArgumentType.string())
                        .suggests((ctx, suggestionsBuilder) -> {
                            Warps.getWarps().forEach((internalName, warp) -> {
                                Component tooltip = Component.empty()
                                        .append(warp.getIcon() != null ? getAtlasSpriteComponent(warp.getIcon()) : Component.empty())
                                        .append(" ")
                                        .append(NewMessageUtil.convertAdventure2Minecraft(warp.getDisplayName()));

                                suggestionsBuilder.suggest(internalName, tooltip);
                            });
                            return suggestionsBuilder.buildFuture();
                        })
                .executes(WarpCommand::execTeleport)
                .then(Commands.argument("対象", EntityArgument.entities())
                        .executes(WarpCommand::execTeleport)));

        dispatcher.register(builder);
    }

    private static int execTeleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> targets;
        if (BrigadierUtil.isArgumentKeyExists(ctx, "対象")) {
            targets = EntityArgument.getEntities(ctx, "対象");
        } else {
            targets = List.of(ctx.getSource().getPlayerOrException());
        }

        String warpName = StringArgumentType.getString(ctx, "名前");
        Warp warp = Warps.getWarp(warpName);
        if (warp == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), net.kyori.adventure.text.Component.text("ワープポイント " + warpName + " は見つかりませんでした"));
            return 0;
        }

        List<? extends Entity> successTargets = targets.stream().filter(entity -> entity.getBukkitEntity().teleport(warp.getLocation())).toList();
        boolean isAllPlayer = successTargets.parallelStream().allMatch(entity -> entity instanceof ServerPlayer);

        net.kyori.adventure.text.Component displayName = net.kyori.adventure.text.Component.empty()
                .append(warp.getIcon() != null ? NewMessageUtil.convertMinecraft2Adventure(getAtlasSpriteComponent(warp.getIcon())) : net.kyori.adventure.text.Component.empty())
                .append(warp.getDisplayName());

        if (successTargets.size() > 1) {
            NewMessageUtil.sendMessage(ctx.getSource(), net.kyori.adventure.text.Component.text(successTargets.size() + (isAllPlayer ? "人" : "のエンティティ") + "を").appendSpace().append(displayName).appendSpace().append(net.kyori.adventure.text.Component.text("へテレポートさせました")));
        } else {
            NewMessageUtil.sendMessage(ctx.getSource(), net.kyori.adventure.text.Component.text("ワープポイント").appendSpace().append(displayName).appendSpace().append(net.kyori.adventure.text.Component.text("へテレポートしました")));
        }
        return 0;
    }

    private static Component getAtlasSpriteComponent(Material material) {
        AtlasSprite sprite = getAtlasSprite(material);
        return Component.object(sprite);
    }

    private static AtlasSprite getAtlasSprite(Material material) {
        if (material.isBlock()) {
            return new AtlasSprite(AtlasIds.BLOCKS, BuiltInRegistries.BLOCK.getKey(CraftMagicNumbers.getBlock(material)).withPrefix("block/"));
        } else {
            return new AtlasSprite(AtlasIds.BLOCKS, BuiltInRegistries.ITEM.getKey(CraftMagicNumbers.getItem(material)).withPrefix("item/"));
        }
    }
}
