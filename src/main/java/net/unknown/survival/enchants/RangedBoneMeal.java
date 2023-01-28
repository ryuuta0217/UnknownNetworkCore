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

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class RangedBoneMeal {
    public static class RangedBoneMealEnchantment extends Enchantment {
        private static RangedBoneMealEnchantment INSTANCE;

        protected RangedBoneMealEnchantment() {
            super(Rarity.COMMON, EnchantmentCategory.WEAPON, new EquipmentSlot[0]);
            Registry.register(BuiltInRegistries.ENCHANTMENT, "ranged_bone_meal", this);
        }

        public static RangedBoneMealEnchantment instance() {
            if (INSTANCE == null) INSTANCE = new RangedBoneMealEnchantment();
            return INSTANCE;
        }

        @Override
        public int getMaxLevel() {
            return 5;
        }

        @Override
        public int getMinCost(int level) {
            return 8 * level;
        }

        @Override
        public int getMaxCost(int level) {
            return super.getMinCost(level) + 16;
        }

        @Override
        public boolean canEnchant(ItemStack stack) {
            return stack.is(Items.BONE_MEAL);
        }
    }
}
