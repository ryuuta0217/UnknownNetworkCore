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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.item.Items;
import net.unknown.core.item.UnknownNetworkItem;
import net.unknown.core.item.UnknownNetworkItemStack;
import net.unknown.survival.wrapper.economy.WrappedEconomy;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MendingSupportStick extends UnknownNetworkItem implements Listener {
    public MendingSupportStick() {
        super(new NamespacedKey("survival", "mending_support_stick"));
    }

    @Override
    public Stack createItemStack() {
        return new Stack(this.createItemStackBuilder(Material.STICK)
                .displayName(Component.text("修繕棒"))
                .build());
    }

    @EventHandler
    public void onUseItem(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.OFF_HAND) return;
        if (!this.equals(event.getItem())) return;
        if (!event.getPlayer().isSneaking()) return;
        event.setCancelled(true);

        Stack stack = new Stack(event.getItem());
        int result = this.applyMending(event.getPlayer(), stack);

        if (result == 0) {
            NewMessageUtil.sendMessage(event.getPlayer(), Component.text("修繕が適用されました。", DefinedTextColor.GREEN));
        } else if (result == 1) {
            NewMessageUtil.sendMessage(event.getPlayer(), Component.text("この修繕棒はもう使用できません。", DefinedTextColor.RED));
        } else if (result == 2) {
            NewMessageUtil.sendMessage(event.getPlayer(), Component.text("経験値が足りません。", DefinedTextColor.RED));
        } else if (result == 3) {
            NewMessageUtil.sendMessage(event.getPlayer(), Component.text("所持金が足りません。", DefinedTextColor.RED));
        }
    }

    /**
     * プレイヤーの所持金と経験値レベルを用いて、手持ちアイテムなどに対して修繕を適用します。
     *
     * @param player 実行プレイヤー
     * @param stack 修繕棒
     * @return 0 の場合は修繕が適用されたことを表します。<br />1 の場合は修繕棒が使用できないことを表します。<br />2 の場合は経験値レベルが足りないことを表します。<br />3 の場合は所持金が足りないことを表します。
     */
    private int applyMending(Player player, Stack stack) {
        if (!stack.canUse()) return 1;
        if (player.getLevel() == 0) return 2;
        if (!WrappedEconomy.INSTANCE.has(player, 1000)) return 3;

        int usesExp = getXpNeededForNextLevel(player.getLevel() - 1);
        player.setLevel(player.getLevel() - 1);
        int remainExp = player.applyMending(usesExp);
        player.giveExp(remainExp, false);
        float expUseRate = (float) remainExp / (float) usesExp;

        int price = expUseRate > 0.5 ? 1000 : 500;
        WrappedEconomy.INSTANCE.withdrawPlayer(player, price);

        stack.setUses(stack.getUses() + 1); // 使用回数++
        return 0;
    }

    public static int getXpNeededForNextLevel(int level) {
        return level >= 30 ? 112 + (level - 30) * 9 : (level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2);
    }

    public static class Stack extends UnknownNetworkItemStack<MendingSupportStick> {
        private static final NamespacedKey USES_KEY = new NamespacedKey("survival", "uses");
        private static final NamespacedKey MAX_USES_KEY = new NamespacedKey("survival", "max-uses");

        private int uses;
        private int maxUses;

        private Stack(ItemStack stack) {
            super(stack, Items.MENDING_SUPPORT_STICK);
            load();
        }

        public void load() {
            this.uses = this.getHandle().getItemMeta().getPersistentDataContainer().getOrDefault(USES_KEY, PersistentDataType.INTEGER, 0);
            this.maxUses = this.getHandle().getItemMeta().getPersistentDataContainer().getOrDefault(MAX_USES_KEY, PersistentDataType.INTEGER, 50);
            this.updateLore();
        }

        private void save() {
            this.getHandle().editMeta(meta -> {
                meta.getPersistentDataContainer().set(USES_KEY, PersistentDataType.INTEGER, this.uses);
                meta.getPersistentDataContainer().set(MAX_USES_KEY, PersistentDataType.INTEGER, this.maxUses);
            });
            this.updateLore();
        }

        private void updateLore() {
            Component usesLine = Component.text("使用回数: " + this.uses + " / " + this.maxUses, DefinedTextColor.GREEN);
            this.getHandle().editMeta(meta -> {
                List<Component> lore = meta.lore();
                if (lore == null) lore = new ArrayList<>();

                if (lore.stream().anyMatch(line -> PlainTextComponentSerializer.plainText().serialize(line).startsWith("使用回数: "))) {
                    lore  = lore.stream().map(line -> {
                        if (PlainTextComponentSerializer.plainText().serialize(line).startsWith("使用回数: ")) {
                            return usesLine;
                        }
                        return line;
                    }).toList();
                } else {
                    lore.add(usesLine);
                }

                meta.lore(lore);
            });
        }

        public boolean canUse() {
            return this.uses < this.maxUses;
        }

        public int getUses() {
            return this.uses;
        }

        public int getMaxUses() {
            return this.maxUses;
        }

        public void setUses(int uses) {
            this.uses = uses;
            save();
        }

        public void setMaxUses(int maxUses) {
            this.maxUses = maxUses;
            save();
        }
    }
}
