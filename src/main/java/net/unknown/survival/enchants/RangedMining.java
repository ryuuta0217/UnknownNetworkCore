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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.util.BlockUtil;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.*;

public class RangedMining implements Listener {
    private static final Map<UUID, BlockFace> FACING = new HashMap<>();

    private static Iterable<BlockPos> withinManhattan(Direction direction, BlockPos center, int range) {
        int x = 0;
        int y = 0;
        int z = 0;

        if (direction == Direction.SOUTH || direction == Direction.NORTH) {
            x = range;
            y = range;
        }

        if (direction == Direction.EAST || direction == Direction.WEST) {
            y = range;
            z = range;
        }

        if (direction == Direction.UP || direction == Direction.DOWN) {
            x = range;
            z = range;
        }
        return BlockPos.withinManhattan(center, x, y, z);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            if (event.getItem() != null && event.getItem().getType().name().contains("_PICKAXE")) {
                FACING.put(event.getPlayer().getUniqueId(), event.getBlockFace());
            }
        }
    }

    public static final Map<UUID, Set<BlockPos>> IGNORE_EVENT = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (IGNORE_EVENT.containsKey(player.getUniqueId())) {
            BlockPos blockPos = MinecraftAdapter.blockPos(event.getBlock().getLocation());
            Set<BlockPos> ignoreEventPoses = IGNORE_EVENT.get(player.getUniqueId());
            if (ignoreEventPoses.contains(blockPos)) {
                ignoreEventPoses.remove(blockPos);
                return;
            }
        }

        if (!FACING.containsKey(player.getUniqueId())) return;

        ItemStack handItem = MinecraftAdapter.ItemStack.itemStack(player.getInventory().getItemInMainHand());
        if (!CustomEnchantUtil.hasEnchantment("§7範囲破壊", player.getInventory().getItemInMainHand())) return;
        int level = CustomEnchantUtil.getEnchantmentLevel(CustomEnchantUtil.getEnchantmentLine("§7範囲破壊", player.getInventory().getItemInMainHand()));

        Direction direction = MinecraftAdapter.direction(FACING.getOrDefault(player.getUniqueId(), player.getFacing()));
        FACING.remove(player.getUniqueId());
        if (!IGNORE_EVENT.containsKey(player.getUniqueId())) IGNORE_EVENT.put(player.getUniqueId(), new HashSet<>());

        Iterable<BlockPos> toBreak = withinManhattan(direction, MinecraftAdapter.blockPos(event.getBlock().getLocation()), level);
        toBreak.forEach(blockPos -> {
            int durabilityRemaining = handItem.getMaxDamage() - handItem.getDamageValue();
            if(durabilityRemaining != 1) {
                IGNORE_EVENT.get(player.getUniqueId()).add(blockPos);
                BlockUtil.destroyBlock(MinecraftAdapter.player(player), MinecraftAdapter.level(event.getBlock().getWorld()), blockPos, MinecraftAdapter.ItemStack.itemStack(player.getInventory().getItemInMainHand()));
            } else {
                player.sendActionBar(Component.text("耐久値が無くなりました", DefinedTextColor.RED));
            }
        });
    }

    public static class RangedMiningEnchantment extends Enchantment {
        private static RangedMiningEnchantment INSTANCE;

        protected RangedMiningEnchantment() {
            super(Rarity.RARE, EnchantmentCategory.DIGGER, new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.MAINHAND});
            this.descriptionId = "範囲破壊";
        }

        public static RangedMiningEnchantment instance() {
            if (INSTANCE == null) INSTANCE = new RangedMiningEnchantment();
            return INSTANCE;
        }

        @Override
        public int getMaxLevel() {
            return 3;
        }

        @Override
        public int getMinCost(int level) {
            return 15 * level;
        }

        @Override
        public int getMaxCost(int level) {
            return super.getMinCost(level) + 30;
        }

        @Override
        public boolean canEnchant(net.minecraft.world.item.ItemStack stack) {
            return stack.getItem() instanceof PickaxeItem && super.canEnchant(stack);
        }

        @Override
        public net.minecraft.network.chat.Component getFullname(int level) {
            MutableComponent mutableComponent = net.minecraft.network.chat.Component.literal(this.getDescriptionId());
            if (this.isCurse()) {
                mutableComponent.withStyle(ChatFormatting.RED);
            } else {
                mutableComponent.withStyle(ChatFormatting.GRAY);
            }

            if (level != 1 || this.getMaxLevel() != 1) {
                mutableComponent.append(" ").append(net.minecraft.network.chat.Component.translatable("enchantment.level." + level));
            }

            return mutableComponent;
        }
    }
}
