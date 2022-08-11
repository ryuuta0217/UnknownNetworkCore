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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.BlockUtil;
import net.unknown.core.util.MinecraftAdapter;
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

        if (IGNORE_EVENT.containsKey(event.getPlayer().getUniqueId())) {
            if (IGNORE_EVENT.get(event.getPlayer().getUniqueId()).contains(breakBlockPos)) {
                IGNORE_EVENT.get(event.getPlayer().getUniqueId()).remove(breakBlockPos);
                return;
            }
        }

        ServerPlayer player = MinecraftAdapter.player(event.getPlayer());
        if (player == null) return;

        /* Enchant test */
        ItemStack selectedITem = player.getMainHandItem();
        org.bukkit.inventory.ItemStack selectedItemB = event.getPlayer().getInventory().getItemInMainHand();
        if (!(selectedITem.getItem() instanceof PickaxeItem) && !(selectedITem.getItem() instanceof AxeItem)) return;
        if (selectedItemB.lore() == null) return;
        List<String> lore = selectedItemB.lore().stream().map(LegacyComponentSerializer.legacySection()::serialize).toList();
        if (lore.stream().noneMatch(s -> s.startsWith("§7一括破壊"))) return;
        /* test end */

        int maxBlocks = (selectedITem.getItem() instanceof PickaxeItem ? 8 : 16) * CustomEnchantUtil.getEnchantmentLevel(lore.stream().filter(s -> s.startsWith("§7一括破壊")).toList().get(0));

        ServerLevel level = MinecraftAdapter.level(event.getBlock().getLocation().getWorld());
        BlockState blockState = MinecraftAdapter.blockState(event.getBlock());
        Block chainDestructTarget = blockState.getBlock();
        if (!CHAIN_DESTRUCT_TARGETS.contains(chainDestructTarget)) return;
        if (blockState.is(BlockTags.LOGS) && !(selectedITem.getItem() instanceof AxeItem)) return;
        if (!player.hasCorrectToolForDrops(blockState)) return;
        if (selectedITem.getItem().getMaxDamage() - selectedITem.getDamageValue() == 1) return;

        Set<BlockPos> toBreak = new HashSet<>();

        BlockUtil.searchBlock(breakBlockPos, maxBlocks, level, chainDestructTarget, toBreak);

        IGNORE_EVENT.put(event.getPlayer().getUniqueId(), new HashSet<>(toBreak));

        AtomicInteger count = new AtomicInteger(1);
        toBreak.forEach(pos -> {
            RunnableManager.runDelayed(() -> {
                if (player.getMainHandItem().equals(selectedITem)) {
                    if (selectedITem.getItem().getMaxDamage() - selectedITem.getDamageValue() > 1) {
                        BlockUtil.destroyBlock(player, level, pos, selectedITem);
                    } else {
                        player.getBukkitEntity().sendActionBar(Component.text("耐久値がなくなりました", DefinedTextColor.RED));
                    }
                }
            }, count.getAndIncrement());
        });
    }
}
