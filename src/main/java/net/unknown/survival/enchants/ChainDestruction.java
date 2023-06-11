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

import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.BlockUtil;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ChainDestruction implements Listener {
    public static final Set<Block> CHAIN_DESTRUCT_TARGETS = new HashSet<>() {{
        add(Blocks.NETHER_QUARTZ_ORE);
        add(Blocks.GLOWSTONE);
    }};

    public static final Set<TagKey<Block>> CHAIN_DESTRUCT_TARGET_TAGS = new HashSet<>() {{
        add(BlockTags.COAL_ORES);
        add(BlockTags.COPPER_ORES);
        add(BlockTags.IRON_ORES);
        add(BlockTags.REDSTONE_ORES);
        add(BlockTags.LAPIS_ORES);
        add(BlockTags.GOLD_ORES);
        add(BlockTags.DIAMOND_ORES);
        add(BlockTags.EMERALD_ORES);

        add(BlockTags.LOGS);
        add(BlockTags.OVERWORLD_NATURAL_LOGS);
    }};

    public static final Map<UUID, Set<BlockPos>> IGNORE_EVENT = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        BlockPos breakBlockPos = MinecraftAdapter.blockPos(event.getBlock().getLocation());

        if (IGNORE_EVENT.containsKey(event.getPlayer().getUniqueId())) {
            if (IGNORE_EVENT.get(event.getPlayer().getUniqueId()).contains(breakBlockPos)) {
                IGNORE_EVENT.get(event.getPlayer().getUniqueId()).remove(breakBlockPos);
                return;
            }
        }

        ServerPlayer player = MinecraftAdapter.player(event.getPlayer());
        if (player == null) return;

        /* Enchant test */
        ItemStack selectedItem = player.getMainHandItem();
        org.bukkit.inventory.ItemStack selectedItemB = event.getPlayer().getInventory().getItemInMainHand();
        if (!(selectedItem.getItem() instanceof PickaxeItem) && !(selectedItem.getItem() instanceof AxeItem)) return;
        if (!CustomEnchantUtil.hasEnchantment("一括破壊", selectedItemB)) return;
        /* test end */

        int maxBlocks = (selectedItem.getItem() instanceof PickaxeItem ? 8 : 32) * CustomEnchantUtil.getEnchantmentLevel(CustomEnchantUtil.getEnchantmentLine("§7一括破壊", selectedItemB));

        ServerLevel level = MinecraftAdapter.level(event.getBlock().getLocation().getWorld());
        BlockState blockState = MinecraftAdapter.blockState(event.getBlock());
        Block chainDestructTarget = blockState.getBlock();
        if (!isValidTarget(chainDestructTarget)) return;
        if (blockState.is(BlockTags.LOGS) && !(selectedItem.getItem() instanceof AxeItem)) return;
        if (!player.hasCorrectToolForDrops(blockState)) return;
        if (selectedItem.getItem().getMaxDamage() - selectedItem.getDamageValue() == 1) return;

        Set<BlockPos> toBreak = new HashSet<>();

        BlockUtil.searchBlock(breakBlockPos, maxBlocks, level, chainDestructTarget, toBreak);

        IGNORE_EVENT.put(player.getUUID(), new HashSet<>());

        AtomicInteger delay = new AtomicInteger(0);

        toBreak.forEach(pos -> {
            RunnableManager.runDelayed(() -> {
                if (player.getMainHandItem().equals(selectedItem)) {
                    if ((selectedItem.getItem().getMaxDamage() - selectedItem.getDamageValue()) > 1) {
                        IGNORE_EVENT.get(player.getUUID()).add(pos);
                        try {
                            player.gameMode.destroyBlock(pos);
                        } catch(Exception ignored) {}
                        IGNORE_EVENT.get(player.getUUID()).remove(pos);
                    } else {
                        player.getBukkitEntity().sendActionBar(Component.text("耐久値がなくなりました", DefinedTextColor.RED));
                    }
                }
            }, delay.getAndAdd(2));
        });

        RunnableManager.runAsyncDelayed(() -> {
            IGNORE_EVENT.getOrDefault(player.getUUID(), new HashSet<>()).clear();
        }, delay.get());
    }

    private static boolean isValidTarget(Block targetBlock) {
        boolean blockMatch = CHAIN_DESTRUCT_TARGETS.contains(targetBlock);
        boolean tagMatch = CHAIN_DESTRUCT_TARGET_TAGS.stream().anyMatch(tag -> targetBlock.builtInRegistryHolder().is(tag));
        return blockMatch || tagMatch;
    }
}
