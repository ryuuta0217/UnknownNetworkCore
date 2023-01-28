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

package net.unknown.survival.enchants.nms;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.DamageEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import net.unknown.core.util.RegistryUtil;

public class DamageEnchant extends Enchantment {
    public static final int ALL = 0;
    public static final int UNDEAD = 1;
    public static final int ARTHROPODS = 2;
    private static final String[] NAMES = new String[] {"all", "undead", "arthropods"};
    private static final int[] MIN_COST = new int[] {1, 5, 5};
    private static final int[] LEVEL_COST = new int[] {11, 8, 8};
    private static final int[] LEVEL_COST_SPAN = new int[] {24, 20, 20};
    public final int type;

    public DamageEnchant(Enchantment.Rarity weight, int typeIndex, EquipmentSlot... slots) {
        super(weight, EnchantmentCategory.WEAPON, slots);
        this.type = typeIndex;
    }

    public static void register() {
        RegistryUtil.forceReplace(BuiltInRegistries.ENCHANTMENT, ResourceLocation.of("minecraft:sharpness", ':'), new DamageEnchant(Rarity.COMMON, 0, EquipmentSlot.MAINHAND));
    }

    @Override
    public int getMinCost(int level) {
        return MIN_COST[this.type] + (level - 1) * LEVEL_COST[this.type];
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) * LEVEL_COST_SPAN[this.type];
    }

    @Override
    public int getMaxLevel() {
        return 10;
    }

    @Override
    public float getDamageBonus(int level, MobType group) {
        return this.type == 0 ? 1.0F + (float) Math.max(0, level - 1) * 0.5F : (this.type == 1 && group == MobType.UNDEAD ? (float) level * 2.5F : (this.type == 2 && group == MobType.ARTHROPOD ? (float) level * 2.5F : 0.0F));
    }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        return !(other instanceof DamageEnchantment);
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof AxeItem || super.canEnchant(stack);
    }

    @Override
    public void doPostAttack(LivingEntity user, Entity target, int level) {
        if (target instanceof LivingEntity) {
            LivingEntity entityliving1 = (LivingEntity) target;

            if (this.type == 2 && level > 0 && entityliving1.getMobType() == MobType.ARTHROPOD) {
                int j = 20 + user.getRandom().nextInt(10 * level);

                entityliving1.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, j, 3), org.bukkit.event.entity.EntityPotionEffectEvent.Cause.ATTACK); // CraftBukkit
            }
        }

    }
}
