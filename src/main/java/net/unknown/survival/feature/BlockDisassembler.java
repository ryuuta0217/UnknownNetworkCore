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
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.unknown.launchwrapper.event.BlockDispenseBeforeEvent;
import net.unknown.launchwrapper.mixininterfaces.IMixinBlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R2.block.CraftBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BlockDisassembler implements Listener {
    private static final Component NAME = Component.literal("Block Disassembler").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);

    @EventHandler
    public void onDispenserShoot(BlockDispenseBeforeEvent event) {
        if (event.getBlockSource().state().getBlock() == Blocks.DISPENSER) {
            DispenserBlockEntity dispenser = event.getBlockSource().blockEntity();
            IMixinBlockEntity mixinBlockEntity = ((IMixinBlockEntity) dispenser);
            if (dispenser.getDisplayName().contains(NAME)) {
                event.setCancelled(true);

                ServerLevel level = ((ServerLevel) dispenser.getLevel());
                Objects.requireNonNull(level);

                BlockPos pos = dispenser.getBlockPos();
                Direction direction = dispenser.getBlockState().getValue(DispenserBlock.FACING);
                BlockPos targetPos = pos.relative(direction);
                BlockState targetState = level.getBlockStateIfLoaded(targetPos);
                if (targetState == null || targetState.isAir()) return; // 空気だったり読み込まれてなかったらやめる

                ItemStack shootItem = event.getItem();
                if (targetState.getBlock() == Blocks.BEDROCK) return; // 岩盤は破壊させないよーん
                if (targetState.requiresCorrectToolForDrops() && !shootItem.getItem().isCorrectToolForDrops(targetState)) return; // 適正ツールが必要なブロックなら適正ツールチェック
                if (shootItem.getMaxDamage() - shootItem.getDamageValue() == 1) return; // 次のブロック破壊で壊れそうならやめる

                FakePlayer player = new FakePlayer(dispenser, mixinBlockEntity.getPlacer());
                player.setItemInHand(InteractionHand.MAIN_HAND, shootItem);

                PlayerInteractEvent piEvent = new PlayerInteractEvent(player.getBukkitEntity(), org.bukkit.event.block.Action.LEFT_CLICK_BLOCK, null, CraftBlock.at(level, targetPos), CraftBlock.notchToBlockFace(player.getDirection()));
                Bukkit.getPluginManager().callEvent(piEvent);

                if (piEvent.isCancelled()) return;

                BlockBreakEvent bbEvent = new BlockBreakEvent(CraftBlock.at(level, targetPos), player.getBukkitEntity());
                Bukkit.getPluginManager().callEvent(bbEvent);

                if (bbEvent.isCancelled()) return;

                shootItem.hurt(1, level.random, null);

                destroyBlockWithDrops(level, targetPos, shootItem).forEach(dropItem -> {
                    if (bbEvent.isDropItems()) Block.popResource(level, targetPos, dropItem);
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

    public static class FakePlayer extends net.unknown.core.entity.FakePlayer {
        private final DispenserBlockEntity dispenser;

        public FakePlayer(DispenserBlockEntity dispenser, @Nullable UUID uniqueId) {
            super((ServerLevel) dispenser.getLevel(), dispenser.getName().getString(), uniqueId);
            this.dispenser = dispenser;
            this.moveTo(dispenser.getBlockPos(), 0.0f, 0.0f);
        }

        @Override
        public Direction getDirection() {
            return this.dispenser.getBlockState().getValue(DispenserBlock.FACING);
        }

        @Nullable
        @Override
        public Component getTabListDisplayName() {
            return NAME;
        }

        @Override
        public Component getName() {
            return NAME;
        }

        @Override
        public Component getDisplayName() {
            return NAME;
        }

        @Override
        public String getScoreboardName() {
            return NAME.getString();
        }

        @Override
        protected Component getTypeName() {
            return NAME;
        }

        @Nullable
        @Override
        public Component getCustomName() {
            return NAME;
        }
    }
}
