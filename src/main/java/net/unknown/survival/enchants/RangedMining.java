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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RangedMining implements Listener {
    private static final Map<UUID, BlockFace> FACING = new HashMap<>();

    private static BlockPos location2BlockPos(Location loc) {
        return new BlockPos(loc.getX(), loc.getY(), loc.getZ());
    }

    private static Location blockPos2Location(BlockPos blockPos, World world) {
        return new Location(world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

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

    @EventHandler
    public void onAttackPlayer(EntityDamageByEntityEvent event) {

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (!FACING.containsKey(player.getUniqueId())) return;

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (!handItem.getType().name().endsWith("_PICKAXE")) return;
        if (handItem.getLore() == null /*&& !((CraftItemStack) handItem).handle.isEnchanted()*/) return;
        if (handItem.getLore().stream().noneMatch(lore -> lore.startsWith("§7採掘範囲拡大")) /*&& EnchantmentHelper.getEnchantments(((CraftItemStack) handItem).handle).containsKey(CustomEnchantments.RANGED_MINING)*/)
            return;
        int range = CustomEnchantUtil.getEnchantmentLevel(handItem.getLore().stream().filter(lore -> lore.startsWith("§7採掘範囲拡大")).toList().get(0));
        /*if(handItem.getLore().stream().anyMatch(lore -> lore.startsWith("§7採掘範囲拡大"))) {
            range = ;
        } else {
            range = /*EnchantmentHelper.getEnchantments(((CraftItemStack) handItem).handle).get(CustomEnchantments.RANGED_MINING)*//* 1;
        }*/

        BlockFace bf = FACING.getOrDefault(event.getPlayer().getUniqueId(), event.getPlayer().getFacing());

        if (bf == BlockFace.SOUTH || bf == BlockFace.NORTH) {
            for (long x = -range; x <= range; x++) {
                for (long y = -range; y <= range; y++) {
                    event.getBlock().getLocation().add(x, y, 0).getBlock().breakNaturally(handItem);
                }
            }
        } else if (bf == BlockFace.EAST || bf == BlockFace.WEST) {
            for (long y = -range; y <= range; y++) {
                for (long z = -range; z <= range; z++) {
                    event.getBlock().getLocation().add(0, y, z).getBlock().breakNaturally(handItem);
                }
            }
        } else if (bf == BlockFace.UP || bf == BlockFace.DOWN) {
            for (long x = -range; x <= range; x++) {
                for (long z = -range; z <= range; z++) {
                    event.getBlock().getLocation().add(x, 0, z).getBlock().breakNaturally(handItem);
                }
            }
        }

        FACING.remove(event.getPlayer().getUniqueId());
    }

    public static class RangedMiningEnchantment extends Enchantment {
        private static RangedMiningEnchantment INSTANCE;

        protected RangedMiningEnchantment() {
            super(Rarity.RARE, EnchantmentCategory.DIGGER, new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.entity.EquipmentSlot.OFFHAND});
            //Registry.register(Registry.ENCHANTMENT, "ranged_mining", this);
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
    }
}
