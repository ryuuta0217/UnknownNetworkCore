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
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
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
        ResourceLocation id;
        if (idStr == null) {
            id = ResourceLocation.fromNamespaceAndPath("unknown-network", "dummy_" + String.valueOf(UUID.randomUUID()).split("-")[0]);
        } else if(idStr.contains(":")) {
            String namespace = idStr.split(":", 2)[0];
            String path = idStr.split(":", 2)[1];
            id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        } else {
            id = ResourceLocation.fromNamespaceAndPath("unknown-network", idStr);
        }

        info.showToast(true);

        AdvancementHolder advancement = Advancement.Builder.advancement()
                .display(info.build())
                .addCriterion("dummy", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                .build(id);

        // Send new advancement to player
        player.connection.send(new ClientboundUpdateAdvancementsPacket(false, List.of(advancement), Collections.emptySet(), Collections.emptyMap()));

        // Grant the advancement to the player (show toast)
        AdvancementProgress progress = new AdvancementProgress();
        progress.update(advancement.value().requirements());
        progress.grantProgress("dummy");
        player.connection.send(new ClientboundUpdateAdvancementsPacket(false, Collections.emptyList(), Collections.emptySet(), Map.of(id, progress)));

        // Remove advancement from the player
        player.connection.send(new ClientboundUpdateAdvancementsPacket(false, Collections.emptyList(), Set.of(id), Collections.emptyMap()));
    }
}
