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

package net.unknown.survival.item;

import net.kyori.adventure.text.Component;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.ViewedGuiBase;
import net.unknown.core.gui.view.PaginationView;
import net.unknown.core.gui.view.View;
import net.unknown.core.item.UnknownNetworkItem;
import net.unknown.core.item.UnknownNetworkItemStack;
import net.unknown.core.managers.RunnableManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PotionEffectItem extends UnknownNetworkItem implements Listener {
    private static BukkitTask TASK;

    public PotionEffectItem() {
        super(new NamespacedKey("survival", "potion_effect"));
    }

    // Open a editor
    /*@EventHandler
    public void onInteractItem(PlayerInteractEvent event) {
        if (!event.getPlayer().isOp()) return;
        if (!event.getPlayer().isSneaking()) return;
        if (event.getHand() != EquipmentSlot.OFF_HAND) return;
        if (event.getItem() == null) return;
        if (!this.equals(event.getItem())) return;
        event.setCancelled(true);
        new Editor(event.getPlayer(), new Stack(event.getItem())).open(event.getPlayer());
    }*/

    public static void startTask() {
        if (TASK != null && !TASK.isCancelled()) {
            TASK.cancel();
            TASK = null;
        }

        TASK = RunnableManager.runRepeating(() -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack item = player.getInventory().getItem(slot);
                    if (Items.POTION_EFFECT_ITEM.equals(item)) {
                        Stack stack = new Stack(item);
                        if (stack.getActiveSlots().contains(slot)) {
                            stack.getPotionEffects().forEach((type, level) -> {
                                int duration = getEffectDuration(type, level);
                                player.addPotionEffect(type.createEffect(duration, level));
                            });
                        }
                    }
                }
            });
        }, 0L, 1L);
    }

    public static void stopTask() {
        if (TASK != null && !TASK.isCancelled()) {
            TASK.cancel();
            TASK = null;
        }
    }

    public Stack.Builder createBuilder(Material itemType) {
        return new Stack.Builder(new Stack(this.createItemStackBuilder(itemType).build()));
    }

    @Override
    @Deprecated
    public Stack createItemStack() {
        return new Stack.Builder(new Stack(this.createItemStackBuilder(Material.PLAYER_HEAD)
                .displayName(Component.text("ポーション効果のあるプレイヤーの頭", DefinedTextColor.GOLD))
                .custom(is -> is.editMeta(meta -> ((SkullMeta) meta).setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString("65922e55-04b3-48c2-a4f7-5b393692adf4")))))
                .lore(Component.text("浮遊 V", DefinedTextColor.GRAY))
                .build()))
                .addPotionEffect(PotionEffectType.LEVITATION, 5)
                .buildUNStack();
    }

    public static int getEffectDuration(PotionEffectType type, int amplifier) {
        if (type == PotionEffectType.NIGHT_VISION) {
            return 20 * 15;
        }

        return 20 + 10;
    }

    public static class Stack extends UnknownNetworkItemStack<UnknownNetworkItem> {
        private static final NamespacedKey ACTIVE_SLOTS_KEY = new NamespacedKey("survival", "active_slots");
        private static final NamespacedKey EFFECTS_KEY = new NamespacedKey("survival", "potion_effects");
        private final Set<EquipmentSlot> activeSlots = new HashSet<>();
        private final Map<PotionEffectType, Integer> effects = new HashMap<>();

        public Stack(ItemStack handle) {
            super(handle, Items.POTION_EFFECT_ITEM);
            this.load();
        }

        private void load() {
            this.activeSlots.clear();
            this.effects.clear();

            PersistentDataContainer dataContainer = this.getHandle().getItemMeta().getPersistentDataContainer();
            PersistentDataContainer effectsContainer = dataContainer.get(EFFECTS_KEY, PersistentDataType.TAG_CONTAINER);

            if (effectsContainer != null) {
                effectsContainer.getKeys().forEach(potionEffectKey -> {
                    PotionEffectType type = PotionEffectType.getByKey(potionEffectKey);
                    Integer level = effectsContainer.get(potionEffectKey, PersistentDataType.INTEGER);

                    if (level == null) {
                        throw new IllegalStateException("What? How did this happen? Potion effect level is null!");
                    }

                    this.effects.put(type, level);
                });
            }

            if (dataContainer.has(ACTIVE_SLOTS_KEY, PersistentDataType.INTEGER_ARRAY) && dataContainer.get(ACTIVE_SLOTS_KEY, PersistentDataType.INTEGER_ARRAY).length > 0) {
                int[] activeSlots = dataContainer.get(ACTIVE_SLOTS_KEY, PersistentDataType.INTEGER_ARRAY);
                if (activeSlots != null) {
                    Arrays.stream(activeSlots)
                            .mapToObj(ordinal -> EquipmentSlot.values()[ordinal])
                            .forEach(this.activeSlots::add);
                }
            } else {
                this.activeSlots.addAll(Arrays.asList(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND));
                this.save();
            }
        }

        private void save() {
            this.getHandle().editMeta(meta -> {
                PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
                PersistentDataContainer effectsContainer = dataContainer.getAdapterContext().newPersistentDataContainer();

                this.effects.forEach((type, level) -> effectsContainer.set(type.getKey(), PersistentDataType.INTEGER, level));

                dataContainer.set(EFFECTS_KEY, PersistentDataType.TAG_CONTAINER, effectsContainer);

                dataContainer.set(ACTIVE_SLOTS_KEY, PersistentDataType.INTEGER_ARRAY, this.activeSlots.stream().mapToInt(EquipmentSlot::ordinal).toArray());
            });
        }

        public boolean hasPotionEffect(PotionEffectType type) {
            return this.effects.containsKey(type);
        }

        public void addPotionEffect(PotionEffectType type, int level) {
            if (this.effects.containsKey(type)) {
                throw new IllegalArgumentException("Potion effect already exists on item!");
            }
            this.effects.put(type, level);
            this.save();
        }

        public void removePotionEffect(PotionEffectType type) {
            if (!this.effects.containsKey(type)) {
                throw new IllegalArgumentException("Potion effect does not exist on item!");
            }
            this.effects.remove(type);
            this.save();
        }

        public Map<PotionEffectType, Integer> getPotionEffects() {
            return Collections.unmodifiableMap(this.effects);
        }

        public Set<EquipmentSlot> getActiveSlots() {
            return Collections.unmodifiableSet(this.activeSlots);
        }

        public Builder asBuilder() {
            return new Builder(this);
        }

        public static class Builder extends ItemStackBuilder {
            private final Stack stack;

            public Builder(Stack stack) {
                super(stack.getHandle());
                this.stack = stack;
            }

            public Builder addPotionEffect(PotionEffectType type, int level) {
                this.stack.addPotionEffect(type, level);
                return this;
            }

            public Builder removePotionEffect(PotionEffectType type) {
                this.stack.removePotionEffect(type);
                return this;
            }

            public Builder clearPotionEffects() {
                this.stack.getPotionEffects().keySet().forEach(this.stack::removePotionEffect);
                return this;
            }

            public Builder setPotionEffects(Map<PotionEffectType, Integer> effects) {
                this.clearPotionEffects();
                effects.forEach(this::addPotionEffect);
                return this;
            }

            public Stack buildUNStack() {
                return this.stack;
            }
        }
    }

    /*public static class Editor extends ViewedGuiBase<View> {
        protected final Stack stack;

        // Gui size: 9 x 6
        // 00 01 02 03 04 05 06 07 08
        // 09 10 11 12 13 14 15 16 17
        // 18 19 20 21 22 23 24 25 26
        // 27 28 29 30 31 32 33 34 35
        // 36 37 38 39 40 41 42 43 44
        // 45 46 47 48 49 50 51 52 53
        public Editor(InventoryHolder owner, Stack stack) {
            super(owner, 54, Component.text("アイテムエディター(P)", DefinedTextColor.DARK_PURPLE), true, null);
            this.setView(new Main(this));
            this.stack = stack;
        }

        public static abstract class ViewBase implements View {
            private final Editor gui;

            public ViewBase(Editor gui) {
                this.gui = gui;
            }

            public Editor getGui() {
                return this.gui;
            }
        }

        // Choose action from [21: add, 22: remove, 23: clear, 31: edit_item]
        public static class Main extends ViewBase {
            public Main(Editor gui) {
                super(gui);
            }

            @Override
            public void initialize() {
                this.getGui().getInventory().setItem(21, DefinedItemStackBuilders.plus().displayName(Component.text("追加", DefinedTextColor.GREEN)).build());
                this.getGui().getInventory().setItem(22, DefinedItemStackBuilders.minus().displayName(Component.text("削除", DefinedTextColor.RED)).build());
                this.getGui().getInventory().setItem(23, DefinedItemStackBuilders.x().displayName(Component.text("全削除", DefinedTextColor.GOLD)).build());
                //TODO: this.getGui().getInventory().setItem(31, new ItemStackBuilder(Material.BOOK).displayName(Component.text("アイテム編集", DefinedTextColor.AQUA)).build());
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                switch(event.getSlot()) {
                    case 21 -> {

                    }
                    case 22 -> {

                    }
                    case 23 -> {

                    }
                    case 31 -> {
                        // TODO: ItemEditor (type displayName in chat, lore, etc...)
                    }
                }
            }

            @Override
            public void clearInventory() {

            }
        }

        public static class PotionEffectChooseView extends PaginationView<PotionEffectType, Editor> {

            private PotionEffectChooseView(Editor gui, Collection<PotionEffectType> data, Function<PotionEffectType, ItemStack> processor, BiConsumer<InventoryClickEvent, PotionEffectType> onClick, BiConsumer<InventoryClickEvent, PaginationView<PotionEffectType, Editor>> createNewAction, BiConsumer<InventoryClickEvent, PaginationView<PotionEffectType, Editor>> previousAction) {
                super(gui, data, processor, onClick, createNewAction, previousAction);
            }

            public static PotionEffectChooseView chooseToAdd() {

            }

            public static PotionEffectChooseView chooseToRemove() {

            }
        }
    }*/
}
