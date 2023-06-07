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

package net.unknown.survival.enchants;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.List;

public class AutoSmelting implements Listener {
    public static boolean DEBUG = false;

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        ServerPlayer player = MinecraftAdapter.player(event.getPlayer());
        if (player == null) return;

        BlockPos blockPos = MinecraftAdapter.blockPos(event.getBlock().getLocation());

        ItemStack selectedItem = player.getMainHandItem();
        org.bukkit.inventory.ItemStack selectedItemB = event.getPlayer().getInventory().getItemInMainHand();
        if (!(selectedItem.getItem() instanceof DiggerItem)) return;
        if (selectedItemB.lore() == null) return;
        if (!CustomEnchantUtil.hasEnchantment("自動精錬", selectedItemB)) return;

        ServerLevel level = MinecraftAdapter.level(event.getBlock().getWorld());
        BlockState blockState = level.getBlockState(blockPos);
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (!player.hasCorrectToolForDrops(blockState)) return;

        FurnaceBlockEntity dummyFurnace = new FurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState()) {
            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        dummyFurnace.setLevel(level);
        if (DEBUG) player.openMenu(dummyFurnace); // for debug
        dummyFurnace.setItem(1, new ItemStack(Items.LAVA_BUCKET, 1));

        List<ItemStack> newDrops = Block.getDrops(blockState, level, blockPos, blockEntity).stream()
                .map(minecraftDropStack -> {
                    dummyFurnace.setItem(0, minecraftDropStack);
                    List<SmeltingRecipe> recipes = MinecraftServer.getServer().getRecipeManager().getRecipesFor(RecipeType.SMELTING, dummyFurnace, level);
                    if (recipes.size() > 0) {
                        ItemStack smeltingResult = recipes.get(0).getResultItem(level.registryAccess());
                        smeltingResult.setCount(minecraftDropStack.getCount());
                        return smeltingResult;
                    }
                    return minecraftDropStack;
                })
                .toList();

        if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && event.isDropItems() && newDrops.size() > 0) {
            event.setDropItems(false);
            newDrops.forEach(minecraftDropStack -> Block.popResource(level, blockPos, minecraftDropStack));
            player.playNotifySound(SoundEvents.GENERIC_BURN, SoundSource.BLOCKS, 0.3f, 1.0f);
        }
    }
}
