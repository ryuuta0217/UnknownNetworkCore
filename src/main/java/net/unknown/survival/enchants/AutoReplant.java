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
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.survival.events.ModifiableBlockBreakEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AutoReplant implements Listener {
    private static final Set<Material> SUPPORTED_CROPS = new HashSet<>() {{
        add(Material.WHEAT);
        add(Material.BEETROOTS);
        add(Material.CARROTS);
        add(Material.POTATOES);
    }};

    private static final String ENCHANTMENT_NAME = "自動再植+回収";

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(ModifiableBlockBreakEvent event) {
        if (!SUPPORTED_CROPS.contains(event.getOriginal().getBlock().getType())) return;
        ServerPlayer player = MinecraftAdapter.player(event.getOriginal().getPlayer());
        if (player == null) return;

        org.bukkit.inventory.ItemStack selectedBukkitStack = event.getOriginal().getPlayer().getInventory().getItemInMainHand();
        ItemStack selectedStack = MinecraftAdapter.ItemStack.itemStack(selectedBukkitStack);
        boolean onlyMaxAge = isOnlyMaxAge(selectedBukkitStack);
        if (!(selectedStack.getItem() instanceof HoeItem)) return;
        if (!CustomEnchantUtil.hasEnchantment(ENCHANTMENT_NAME, selectedBukkitStack)) return;
        Block block = event.getOriginal().getBlock();
        if (!(block.getBlockData() instanceof Ageable ageable)) return;

        if (ageable.getAge() != ageable.getMaximumAge()) {
            if (onlyMaxAge) event.getOriginal().setCancelled(true);
        } else {
            event.getOriginal().setCancelled(true);
            event.getOriginal().setDropItems(false);

            List<org.bukkit.inventory.ItemStack> drops = event.getOriginalDrops();
            if (drops.size() == 0) return;

            if (event.getOriginal().getBlock().getType() == Material.WHEAT) {
                drops.stream()
                        .filter(drop -> drop.getType() == Material.WHEAT_SEEDS)
                        .findAny()
                        .ifPresent(drop -> drop.setAmount(drop.getAmount() - 1));
            } else if (event.getOriginal().getBlock().getType() == Material.BEETROOTS) {
                drops.stream()
                        .filter(drop -> drop.getType() == Material.BEETROOT_SEEDS)
                        .findAny()
                        .ifPresent(drop -> drop.setAmount(drop.getAmount() - 1));
            } else {
                org.bukkit.inventory.ItemStack drop = drops.get(drops.size() - 1);
                drop.setAmount(drop.getAmount() - 1);
            }
            drops.removeIf(bukkitStack -> bukkitStack.getType() == Material.AIR);

            event.getOriginal().getPlayer().getInventory().addItem(drops.toArray(new org.bukkit.inventory.ItemStack[0])).forEach((slot, stack) -> {

                event.getOriginal().getPlayer().getWorld().dropItem(event.getOriginal().getBlock().getLocation(), stack);
            });

            ageable.setAge(0);
            block.setBlockData(ageable);
        }
    }

    @EventHandler
    public void onHeldItemChanged(PlayerItemHeldEvent event) {
        if (event.getPreviousSlot() == event.getNewSlot()) return;
        if (!event.getPlayer().isSneaking()) return;
        org.bukkit.inventory.ItemStack selectedBukkitStack = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
        ItemStack selectedStack = MinecraftAdapter.ItemStack.itemStack(selectedBukkitStack);

        if (!(selectedStack.getItem() instanceof HoeItem hoe)) return;
        if (!CustomEnchantUtil.hasEnchantment(ENCHANTMENT_NAME, selectedBukkitStack)) return;
        ItemMeta meta = selectedBukkitStack.getItemMeta();
        if (!meta.hasLore()) return;
        meta.lore(meta.lore().stream().map(lore -> {
            String loreStr = PlainTextComponentSerializer.plainText().serialize(lore);
            if (loreStr.startsWith("成長済のみ")) {
                String statusStr = loreStr.split(": ?", 2)[1];
                boolean onlyMaxAge = !statusStr.equals("有効"); // Toggle status
                event.setCancelled(true);
                event.getPlayer().sendActionBar(Component.empty()
                        .append(selectedBukkitStack.displayName())
                        .append(Component.text(" モードを切り替えました: "))
                        .append(onlyMaxAge ? Component.text("成長済のみ回収", DefinedTextColor.GREEN) : Component.text("成長済以外も回収", DefinedTextColor.RED)));
                return Component.empty()
                        .append(Component.text("成長済のみ: ", Style.style(DefinedTextColor.GREEN, TextDecoration.ITALIC.withState(false))))
                        .append(onlyMaxAge ? Component.text("有効", Style.style(DefinedTextColor.GREEN, TextDecoration.ITALIC.withState(false))) : Component.text("無効", Style.style(DefinedTextColor.RED, TextDecoration.ITALIC.withState(false))));
            }
            return lore;
        }).collect(Collectors.toList()));
        selectedBukkitStack.setItemMeta(meta);
    }

    private static boolean isOnlyMaxAge(org.bukkit.inventory.ItemStack bukkitStack) {
        if (!bukkitStack.hasItemMeta()) return false;
        ItemMeta meta = bukkitStack.getItemMeta();
        if (!meta.hasLore()) return false;
        return meta.lore().stream().anyMatch(lore -> {
            String loreStr = PlainTextComponentSerializer.plainText().serialize(lore);
            if (loreStr.startsWith("成長済のみ")) {
                String statusStr = loreStr.split(": ?", 2)[1];
                return statusStr.equals("有効");
            }
            return false;
        });
    }
}
