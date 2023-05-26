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

package net.unknown.core.util;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v1_19_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_19_R2.event.CraftEventFactory;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BlockUtil {
    public static void searchBlock(BlockPos center, int maxBlockCount, Level level, Block searchTarget, Set<BlockPos> data) {
        if (searchTarget == Blocks.AIR) return;
        BlockPos.withinManhattan(center, 1, 1, 1).forEach(pos -> {
            BlockPos immPos = pos.immutable();
            BlockState state = level.getBlockState(immPos);
            if (state.is(searchTarget) && !data.contains(immPos) && data.size() <= maxBlockCount && !center.equals(immPos)) {
                data.add(immPos);
                searchBlock(immPos, maxBlockCount, level, searchTarget, data);
            }
        });
    }

    public static void searchBlockWithinManhattan(BlockPos center, int rangeX, int rangeY, int rangeZ, Level level, Block searchTarget, Set<BlockPos> data) {
        if (searchTarget == Blocks.AIR) return;
        BlockPos.withinManhattan(center, rangeX, rangeY, rangeZ).forEach(pos -> {
            BlockPos immPos = pos.immutable();
            BlockState state = level.getBlockStateIfLoaded(immPos);
            if (state != null && state.is(searchTarget) && !data.contains(immPos) && !center.equals(immPos)) {
                data.add(immPos);
            }
        });
    }

    public static void destroyBlockWithNoEvent(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack usedTool) {
        BlockState blockState = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        Block block = blockState.getBlock();

        if (block instanceof GameMasterBlock && !player.canUseGameMasterBlocks() && !player.isCreative() && !(block instanceof CommandBlock && player.getBukkitEntity().hasPermission("minecraft.commandblock"))) {
            level.sendBlockUpdated(pos, blockState, blockState, Block.UPDATE_ALL);
        } else {
            if (player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) return;

            block.playerWillDestroy(level, pos, blockState, player);
            boolean hasRemoved = level.removeBlock(pos, false);

            if (hasRemoved) {
                block.destroy(level, pos, blockState);
            }

            boolean isCorrectTool = player.hasCorrectToolForDrops(blockState);

            if (!player.isCreative()) {
                usedTool.mineBlock(level, blockState, pos, player);

                if (hasRemoved && isCorrectTool) {
                    block.playerDestroy(level, player, pos, blockState, blockEntity, usedTool);
                    block.popExperience(level, pos, block.getExpDrop(blockState, level, pos, usedTool, true), player);
                }
            }

            if (hasRemoved && isCorrectTool && block instanceof BeehiveBlock && blockEntity instanceof BeehiveBlockEntity hive) {
                CriteriaTriggers.BEE_NEST_DESTROYED.trigger(player, blockState, usedTool, hive.getOccupantCount());
            }
        }
    }

    public static void destroyBlock(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack usedTool) {
        org.bukkit.block.Block bukkitBlock = CraftBlock.at(level, pos);
        BlockBreakEvent event = new BlockBreakEvent(bukkitBlock, player.getBukkitEntity());

        BlockState blockState = level.getBlockState(pos);
        Block block = blockState.getBlock();

        if (!event.isCancelled() && !player.isCreative() && player.hasCorrectToolForDrops(blockState)) {
            event.setExpToDrop(block.getExpDrop(blockState, level, pos, usedTool, true));
        }

        level.getCraftServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            player.connection.send(new ClientboundBlockUpdatePacket(level, pos));

            for (Direction direction : Direction.values()) {
                player.connection.send(new ClientboundBlockUpdatePacket(level, pos.relative(direction)));
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                player.connection.send(blockEntity.getUpdatePacket());
            }

            return;
        }

        blockState = level.getBlockState(pos); // CraftBukkit - update state from plugins
        if (blockState.isAir()) return; // CraftBukkit - A plugin set block to air without cancelling
        BlockEntity blockEntity = level.getBlockEntity(pos);
        block = blockState.getBlock();

        if (block instanceof GameMasterBlock && !player.canUseGameMasterBlocks() && !player.isCreative() && !(block instanceof CommandBlock && player.getBukkitEntity().hasPermission("minecraft.commandblock"))) {
            level.sendBlockUpdated(pos, blockState, blockState, Block.UPDATE_ALL);
        } else {
            if (player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) return;

            org.bukkit.block.BlockState bukkitBlockState = bukkitBlock.getState();
            level.captureDrops = new ArrayList<>();
            block.playerWillDestroy(level, pos, blockState, player);

            boolean removed = level.removeBlock(pos, false);

            if (removed) {
                block.destroy(level, pos, blockState);
            }

            ItemStack usedToolCopy = usedTool.copy();
            boolean isCorrectTool = !blockState.requiresCorrectToolForDrops() || usedTool.isCorrectToolForDrops(blockState);

            if (!player.isCreative()) {
                usedTool.mineBlock(level, blockState, pos, player);
                if (removed && isCorrectTool && event.isDropItems()) {
                    block.playerDestroy(level, player, pos, blockState, blockEntity, usedToolCopy);
                }
            }

            List<ItemEntity> itemsToDrop = level.captureDrops;
            itemsToDrop.forEach(itemEntity -> itemEntity.moveTo(player.position()));
            level.captureDrops = null;
            if (event.isDropItems()) {
                CraftEventFactory.handleBlockDropItemEvent(bukkitBlock, bukkitBlockState, player, itemsToDrop);
            }

            blockState.getBlock().popExperience(level, pos, event.getExpToDrop(), player);

            if (isCorrectTool && event.isDropItems() && block instanceof BeehiveBlock && blockEntity instanceof BeehiveBlockEntity beeHive) {
                CriteriaTriggers.BEE_NEST_DESTROYED.trigger(player, blockState, usedToolCopy, beeHive.getOccupantCount());
            }
        }
    }
}
