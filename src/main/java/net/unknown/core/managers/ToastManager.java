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

package net.unknown.core.managers;

import io.papermc.paper.advancement.AdvancementDisplay;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.*;
import net.minecraft.advancements.triggers.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.*;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.*;
import net.unknown.core.builder.advancement.DisplayInfoBuilder;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

public class ToastManager {
    public static void showAdvancementToast(Player player, AdvancementDisplay.Frame type, ItemStack icon, Component title) {
        showAdvancementToast(player, AdvancementType.valueOf(type.name()), icon, title);
    }

    public static void showAdvancementToast(Player player, AdvancementType type, ItemStack icon, Component title) {
        showAdvancementToast(MinecraftAdapter.player(player), null, new DisplayInfoBuilder()
                .type(type)
                .title(NewMessageUtil.convertAdventure2Minecraft(title))
                .description(NewMessageUtil.convertAdventure2Minecraft(title))
                .icon(MinecraftAdapter.ItemStack.itemStack(icon)));
    }

    public static void showAdvancementToast(ServerPlayer player, @Nullable String idStr, DisplayInfoBuilder info) {
        Identifier id;
        if (idStr == null) {
            id = Identifier.fromNamespaceAndPath("unknown-network", "dummy_" + String.valueOf(UUID.randomUUID()).split("-")[0]);
        } else if(idStr.contains(":")) {
            String namespace = idStr.split(":", 2)[0];
            String path = idStr.split(":", 2)[1];
            id = Identifier.fromNamespaceAndPath(namespace, path);
        } else {
            id = Identifier.fromNamespaceAndPath("unknown-network", idStr);
        }

        info.showToast(true);

        AdvancementHolder advancement = Advancement.Builder.advancement()
                .display(info.build())
                .addCriterion("dummy", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                .build(id);

        // Send new advancement to player
        player.connection.send(new ClientboundUpdateAdvancementsPacket(false, List.of(advancement), Collections.emptySet(), Collections.emptyMap(), true));

        // Grant the advancement to the player (show toast)
        AdvancementProgress progress = new AdvancementProgress();
        progress.update(advancement.value().requirements());
        progress.grantProgress("dummy");
        player.connection.send(new ClientboundUpdateAdvancementsPacket(false, Collections.emptyList(), Collections.emptySet(), Map.of(id, progress), true));

        // Remove advancement from the player
        player.connection.send(new ClientboundUpdateAdvancementsPacket(false, Collections.emptyList(), Set.of(id), Collections.emptyMap(), true));
    }

    public static void showRecipeUnlockToast(ServerPlayer player, RecipeType<?> type, Ingredient ingredient, RecipeCategory category, @Nullable CookingBookCategory cookingCategory, Item item, int count) {
        RecipeManager recipeManager = MinecraftServer.getServer().getRecipeManager();
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("unknown-network", "recipes/dummy_" + String.valueOf(UUID.randomUUID()).split("-")[0]));
        Map<ResourceKey<Recipe<?>>, Recipe<?>> recipes = new HashMap<>();

        RecipeOutput recipeOutput = new RecipeOutput() {
            @Override
            public void accept(ResourceKey<Recipe<?>> key, Recipe<?> recipe, @org.jetbrains.annotations.Nullable AdvancementHolder advancement) {
                recipes.put(key, recipe);
            }

            @Override
            public Advancement.Builder advancement() {
                return net.minecraft.advancements.Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
            }

            @Override
            public void includeRootAdvancement() {

            }
        };

        if (type == RecipeType.SMELTING) {
            Objects.requireNonNull(cookingCategory, "Cooking category must be provided for smelting recipe");
            SimpleCookingRecipeBuilder.smelting(ingredient, category, cookingCategory, item, 0f, Integer.MAX_VALUE).unlockedBy("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance())).save(recipeOutput, key);
        } else if (type == RecipeType.BLASTING) {
            Objects.requireNonNull(cookingCategory, "Cooking category must be provided for blasting recipe");
            SimpleCookingRecipeBuilder.blasting(ingredient, category, cookingCategory, item, 0f, Integer.MAX_VALUE).unlockedBy("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance())).save(recipeOutput, key);
        } else if (type == RecipeType.SMOKING) {
            SimpleCookingRecipeBuilder.smoking(ingredient, category, item, 0f, Integer.MAX_VALUE).unlockedBy("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance())).save(recipeOutput, key);
        } else if (type == RecipeType.CAMPFIRE_COOKING) {
            SimpleCookingRecipeBuilder.campfireCooking(ingredient, category, item, 0f, Integer.MAX_VALUE).unlockedBy("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance())).save(recipeOutput, key);
        } else if (type == RecipeType.STONECUTTING) {
            SingleItemRecipeBuilder.stonecutting(ingredient, category, item, count).unlockedBy("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance())).save(recipeOutput, key);
        } else if (type == RecipeType.SMITHING) {
            SmithingTransformRecipeBuilder.smithing(ingredient, ingredient, ingredient, category, item).unlocks("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance())).save(recipeOutput, key);
        } else {
            ShapelessRecipeBuilder.shapeless(MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.ITEM), category, item, count).requires(ingredient).unlockedBy("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance())).save(recipeOutput, key);
        }

        recipes.forEach((k, v) -> {
            RecipeHolder<Recipe<?>> recipe = new RecipeHolder<>(k, v);
            recipeManager.addRecipe(recipe);
            player.awardRecipes(List.of(recipe));
            player.resetRecipes(List.of(recipe));
            recipeManager.removeRecipe(recipe.id());
        });
    }
}
