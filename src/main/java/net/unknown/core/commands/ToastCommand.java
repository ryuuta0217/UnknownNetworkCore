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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.unknown.core.builder.advancement.DisplayInfoBuilder;
import net.unknown.core.enums.Permissions;
import net.unknown.core.managers.ToastManager;
import net.unknown.core.util.NewMessageUtil;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

// /toast <players> <advancement|recipe> <type> <item> <message>
public class ToastCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("toast");
        builder.requires(Permissions.COMMAND_TOAST::check);
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> targetsNode = Commands.argument("targets", EntityArgument.players());

        for (AdvancementType type : AdvancementType.values()) {
            targetsNode.then(Commands.literal("advancement")
                    .then(Commands.literal(type.name().toLowerCase())
                            .then(Commands.argument("icon", ItemArgument.item(buildContext))
                                    .then(Commands.argument("title", ComponentArgument.textComponent(buildContext))
                                            .then(Commands.argument("description", ComponentArgument.textComponent(buildContext))
                                                    .executes(ctx -> executeAdvancement(ctx, type)))))));
        }

        Map<String, RecipeType<?>> recipeTypes = Map.of(
                "CRAFTING", RecipeType.CRAFTING,
                "SMELTING", RecipeType.SMELTING,
                "BLASTING", RecipeType.BLASTING,
                "SMOKING", RecipeType.SMOKING,
                "CAMPFIRE_COOKING", RecipeType.CAMPFIRE_COOKING,
                "STONECUTTING", RecipeType.STONECUTTING,
                "SMITHING", RecipeType.SMITHING
        );
        recipeTypes.forEach((name, type) -> {
            for (RecipeCategory category : RecipeCategory.values()) {
                LiteralArgumentBuilder<CommandSourceStack> subTree = Commands.literal(category.name().toLowerCase());
                if (type == RecipeType.SMELTING || type == RecipeType.BLASTING) {
                    for (CookingBookCategory cookingCategory : CookingBookCategory.values()) {
                        subTree.then(Commands.literal(cookingCategory.name().toLowerCase())
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeRecipe(ctx, type, category, cookingCategory)))));
                    }
                } else {
                    subTree.then(Commands.argument("item", ItemArgument.item(buildContext))
                            .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                    .executes(ctx -> executeRecipe(ctx, type, category, null))));
                }

                targetsNode.then(Commands.literal("recipe")
                        .then(Commands.literal(name.toLowerCase())
                                .then(Commands.argument("ingredient", ItemArgument.item(buildContext))
                                        .then(subTree))));
            }
        });

        builder.then(targetsNode);

        dispatcher.register(builder);
    }

    private static int executeAdvancement(CommandContext<CommandSourceStack> ctx, AdvancementType type) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        ItemStack icon = ItemArgument.getItem(ctx, "icon").createItemStack(1);
        Component title = ComponentArgument.getRawComponent(ctx, "title");
        Component description = ComponentArgument.getRawComponent(ctx, "description");

        targets.forEach(player -> {
            ToastManager.showAdvancementToast(player, null, new DisplayInfoBuilder()
                    .type(type)
                    .icon(icon)
                    .title(title)
                    .description(description));
        });
        NewMessageUtil.sendMessage(ctx.getSource(), net.kyori.adventure.text.Component.text((targets.size() == 1 ? targets.iterator().next().displayName : targets.size()) + "にトースト通知を送信しました"));
        return targets.size();
    }

    private static int executeRecipe(CommandContext<CommandSourceStack> ctx, RecipeType<?> type, RecipeCategory category, @Nullable CookingBookCategory cookingCategory) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        Ingredient ingredient = Ingredient.of(ItemArgument.getItem(ctx, "ingredient").item().value());
        Item item = ItemArgument.getItem(ctx, "item").item().value();
        int count = IntegerArgumentType.getInteger(ctx, "count");

        targets.forEach(player -> {
            ToastManager.showRecipeUnlockToast(player, type, ingredient, category, cookingCategory, item, count);
        });
        NewMessageUtil.sendMessage(ctx.getSource(), net.kyori.adventure.text.Component.text((targets.size() == 1 ? targets.iterator().next().displayName : targets.size()) + "にトースト通知を送信しました"));
        return targets.size();
    }
}
