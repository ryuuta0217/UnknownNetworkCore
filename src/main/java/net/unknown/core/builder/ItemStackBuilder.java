/*
 * Copyright (c) 2021 Unknown Network Developers and contributors.
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
 *     loss of use data or profits; or business interpution) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.core.builder;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ItemStackBuilder {
    private final ItemStack original;

    public ItemStackBuilder(Material material) {
        this.original = new ItemStack(material);
    }

    public ItemStackBuilder(Material material, int amount) {
        this.original = new ItemStack(material, amount);
    }

    public ItemStackBuilder enchantments(Map<Enchantment, Integer> enchantments) {
        if(this.original.getEnchantments().size() > 0) {
            this.original.getEnchantments().forEach((ench, lvl) -> this.original.removeEnchantment(ench));
        }
        this.original.addEnchantments(enchantments);
        return this;
    }

    public ItemStackBuilder addEnchantment(Enchantment enchant, int level) {
        this.original.addEnchantment(enchant, level);
        return this;
    }

    public ItemStackBuilder itemFlags(ItemFlag... flags) {
        this.original.getItemFlags().forEach(this.original::removeItemFlags);
        this.original.addItemFlags(flags);
        return this;
    }

    public ItemStackBuilder addItemFlag(ItemFlag flag) {
        this.original.addItemFlags(flag);
        return this;
    }

    public ItemStackBuilder amount(int amount) {
        this.original.setAmount(amount);
        return this;
    }

    public ItemStackBuilder displayName(Component displayName) {
        ItemMeta meta = this.original.getItemMeta();
        meta.displayName(displayName);
        this.original.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder lore(Component... lore) {
        ItemMeta meta = this.original.getItemMeta();
        meta.lore(List.of(lore));
        this.original.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder custom(Consumer<ItemStack> custom) {
        custom.accept(this.original);
        return this;
    }

    public ItemStack build() {
        return this.original;
    }
}
