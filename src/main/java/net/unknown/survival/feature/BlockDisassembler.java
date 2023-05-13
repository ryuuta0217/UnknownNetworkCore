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

package net.unknown.survival.feature;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.unknown.launchwrapper.event.BlockDispenseBeforeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class BlockDisassembler implements Listener {
    @EventHandler
    public void onDispenserShoot(BlockDispenseBeforeEvent event) {
        if (event.getBlockSource().getBlockState().getBlock() == Blocks.DISPENSER && event.getBlockSource().getEntity() instanceof DispenserBlockEntity dispenser) {
            if (dispenser.getDisplayName().contains(Component.literal("Block Disassembler").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))) {
                ServerLevel level = ((ServerLevel) dispenser.getLevel());
                BlockPos pos = dispenser.getBlockPos();
                Direction direction = dispenser.getBlockState().getValue(DispenserBlock.FACING);
                BlockPos targetPos = pos.relative(direction);
                ItemStack shootItem = event.getItem();
                shootItem.hurt(1, level.random, null);
                event.setCancelled(true);
                destroyBlockWithDrops(level, targetPos, shootItem).forEach(dropItem -> {
                    Block.popResource(level, targetPos, dropItem);
                });
            }
        }
    }

    public static List<ItemStack> destroyBlockWithDrops(ServerLevel level, BlockPos blockPos, ItemStack useItem) {
        BlockState currentState = level.getBlockState(blockPos);
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, blockPos, Block.getId(currentState));
        List<ItemStack> drops = Block.getDrops(currentState, level, blockPos, currentState.hasBlockEntity() ? level.getBlockEntity(blockPos) : null, null, useItem);
        level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL, 512);
        level.gameEvent(GameEvent.BLOCK_DESTROY, blockPos, GameEvent.Context.of(null, currentState));
        return drops;
    }
}
