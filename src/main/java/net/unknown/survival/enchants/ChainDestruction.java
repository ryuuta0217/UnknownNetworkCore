/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
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

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.craftbukkit.v1_19_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_19_R1.event.CraftEventFactory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ChainDestruction implements Listener {
    public static final Set<Block> CHAIN_DESTRUCT_TARGETS = new HashSet<>() {{
        add(Blocks.COAL_ORE);
        add(Blocks.DEEPSLATE_COAL_ORE);

        add(Blocks.COPPER_ORE);
        add(Blocks.DEEPSLATE_COPPER_ORE);

        add(Blocks.IRON_ORE);
        add(Blocks.DEEPSLATE_IRON_ORE);

        add(Blocks.REDSTONE_ORE);
        add(Blocks.DEEPSLATE_REDSTONE_ORE);

        add(Blocks.LAPIS_ORE);
        add(Blocks.DEEPSLATE_LAPIS_ORE);

        add(Blocks.GOLD_ORE);
        add(Blocks.DEEPSLATE_GOLD_ORE);

        add(Blocks.DIAMOND_ORE);
        add(Blocks.DEEPSLATE_DIAMOND_ORE);

        add(Blocks.EMERALD_ORE);
        add(Blocks.DEEPSLATE_EMERALD_ORE);

        add(Blocks.OAK_LOG);
        add(Blocks.BIRCH_LOG);
        add(Blocks.SPRUCE_LOG);
        add(Blocks.JUNGLE_LOG);
        add(Blocks.ACACIA_LOG);
        add(Blocks.DARK_OAK_LOG);
        add(Blocks.MANGROVE_LOG);
    }};

    public static final Map<UUID, Set<BlockPos>> IGNORE_EVENT = new HashMap<>();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        BlockPos breakBlockPos = MinecraftAdapter.blockPos(event.getBlock().getLocation());

        if(IGNORE_EVENT.containsKey(event.getPlayer().getUniqueId())) {
            if(IGNORE_EVENT.get(event.getPlayer().getUniqueId()).contains(breakBlockPos)) {
                IGNORE_EVENT.get(event.getPlayer().getUniqueId()).remove(breakBlockPos);
                return;
            }
        }

        ServerPlayer player = MinecraftAdapter.player(event.getPlayer());
        if(player == null) return;

        /* Enchant test */
        ItemStack selectedITem = player.getMainHandItem();
        org.bukkit.inventory.ItemStack selectedItemB = event.getPlayer().getInventory().getItemInMainHand();
        if(!(selectedITem.getItem() instanceof PickaxeItem) && !(selectedITem.getItem() instanceof AxeItem)) return;
        if (selectedItemB.lore() == null) return;
        List<String> lore = selectedItemB.lore().stream().map(LegacyComponentSerializer.legacySection()::serialize).toList();
        if (lore.stream().noneMatch(s -> s.startsWith("§7一括破壊"))) return;
        int maxBlocks = 8 * CustomEnchantUtil.getEnchantmentLevel(lore.stream().filter(s -> s.startsWith("§7一括破壊")).toList().get(0));

        ServerLevel level = MinecraftAdapter.level(event.getBlock().getLocation().getWorld());
        BlockState blockState = MinecraftAdapter.blockState(event.getBlock());
        Block chainDestructTarget = blockState.getBlock();
        if (!CHAIN_DESTRUCT_TARGETS.contains(chainDestructTarget)) return;
        if (blockState.is(BlockTags.LOGS) && !(selectedITem.getItem() instanceof AxeItem)) return;
        if (!player.hasCorrectToolForDrops(blockState)) return;
        if (selectedITem.getItem().getMaxDamage() - selectedITem.getDamageValue() == 1) return;

        Set<BlockPos> toBreak = new HashSet<>();

        searchBlock(breakBlockPos, maxBlocks, level, chainDestructTarget, toBreak);

        IGNORE_EVENT.put(event.getPlayer().getUniqueId(), new HashSet<>(toBreak));

        AtomicInteger count = new AtomicInteger(1);
        toBreak.forEach(pos -> {
            RunnableManager.runDelayed(() -> {
                if(player.getMainHandItem().equals(selectedITem)) {
                    if (selectedITem.getItem().getMaxDamage() - selectedITem.getDamageValue() > 1) {
                        destroyBlock(player, level, pos, selectedITem);
                    }
                }
            }, count.getAndIncrement());
        });
    }

    private static void searchBlock(BlockPos center, int maxBlockCount, Level level, Block searchTarget, Set<BlockPos> data) {
        if(searchTarget == Blocks.AIR) return;
        BlockPos.withinManhattan(center, 1, 1, 1).forEach(pos -> {
            BlockPos immPos = pos.immutable();
            BlockState state = level.getBlockState(immPos);
            if(state.is(searchTarget) && !data.contains(immPos) && data.size() <= maxBlockCount && !center.equals(immPos)) {
                data.add(immPos);
                searchBlock(immPos, maxBlockCount, level, searchTarget, data);
            }
        });
    }

    private static void destroyBlockWithNoEvent(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack usedTool) {
        BlockState blockState = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        Block block = blockState.getBlock();

        if (block instanceof GameMasterBlock && !player.canUseGameMasterBlocks() && !player.isCreative() && !(block instanceof CommandBlock && player.getBukkitEntity().hasPermission("minecraft.commandblock"))) {
            level.sendBlockUpdated(pos, blockState, blockState, Block.UPDATE_ALL);
        } else {
            if(player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) return;

            block.playerWillDestroy(level, pos, blockState, player);
            boolean hasRemoved = level.removeBlock(pos, false);

            if (hasRemoved) {
                block.destroy(level, pos, blockState);
            }

            boolean isCorrectTool = player.hasCorrectToolForDrops(blockState);

            if(!player.isCreative()) {
                usedTool.mineBlock(level, blockState, pos, player);

                if(hasRemoved && isCorrectTool) {
                    block.playerDestroy(level, player, pos, blockState, blockEntity, usedTool);
                    block.popExperience(level, pos, block.getExpDrop(blockState, level, pos, usedTool, true), player);
                }
            }

            if(hasRemoved && isCorrectTool && block instanceof BeehiveBlock && blockEntity instanceof BeehiveBlockEntity hive) {
                CriteriaTriggers.BEE_NEST_DESTROYED.trigger(player, blockState, usedTool, hive.getOccupantCount());
            }
        }
    }

    private static void destroyBlock(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack usedTool) {
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

            for(Direction direction : Direction.values()) {
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
            if(player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) return;

            org.bukkit.block.BlockState bukkitBlockState = bukkitBlock.getState();
            level.captureDrops = new ArrayList<>();
            block.playerWillDestroy(level, pos, blockState, player);

            boolean removed = level.removeBlock(pos, false);

            if(removed) {
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
