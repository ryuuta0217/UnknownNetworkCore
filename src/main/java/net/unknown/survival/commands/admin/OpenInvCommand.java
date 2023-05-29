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

package net.unknown.survival.commands.admin;

import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class OpenInvCommand {
    // TODO When player is online, use #getInventory to Map the Inventory contents.
    // TODO When player is offline, use #loadPlayerData(UUID) and #savePlayerData(UUID, CompoundTag) to read Inventory contents.

    // TODO When target inventory changed, synchronize content to viewer inventory. (only target if online)
    // TODO When viewer inventory changed, synchronize content to target inventory. (it is easy, overwrite the target inventory contents)

    public static WrappedInventory getInventory(UUID playerUniqueId) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUniqueId);
        WrappedInventory inv = new WrappedInventory(Bukkit.createInventory(null, 45, Component.text(player.getName() + " のインベントリ")));
        if (player.isOnline()) {
            PlayerInventory playerInventory = Bukkit.getPlayer(playerUniqueId).getInventory();
            IntStream.rangeClosed(0, playerInventory.getSize()).forEach(slot -> inv.setItem(slot >= 36 && slot <= 39 ? (39 - slot) + 36 : slot, playerInventory.getItem(slot)));

            inv.appendOnClickHandler((event) -> {
                // Synchronize the inventory with the player's inventory.
                // When offline, save the inventory to the player's data file.
            });
        } else {
            CompoundTag data = loadPlayerData(playerUniqueId);
            if (data != null && data.contains("Inventory")) {
                ListTag inventoryList = data.getList("Inventory", Tag.TAG_COMPOUND);

                for(int i = 0; i < inventoryList.size(); i++) {
                    CompoundTag itemStackTag = inventoryList.getCompound(i);
                    int slot = itemStackTag.getByte("Slot") & 255;
                    ItemStack itemStack = ItemStack.of(itemStackTag);
                    inv.setItem(convInvSlotMinecraft2Bukkit(slot), MinecraftAdapter.ItemStack.itemStack(itemStack));
                }
            }

            inv.appendOnClickHandler((event) -> {
                // Save the inventory to the player's data file.
                // When online, synchronize the inventory with the player's inventory.
            });
        }
        return inv;
    }

    public static CompoundTag loadPlayerData(UUID playerUniqueId) {
        File playerDir = MinecraftServer.getServer().getPlayerList().playerIo.getPlayerDir();
        File playerFile = new File(playerDir, playerUniqueId + ".dat");
        if (playerFile.exists()) {
            try {
                return NbtIo.readCompressed(playerFile);
            } catch (IOException ignored) {}
        }

        return null;
    }

    public static boolean savePlayerData(UUID playerUniqueId, CompoundTag playerData) {
        File playerDir = MinecraftServer.getServer().getPlayerList().playerIo.getPlayerDir();
        File playerFile = new File(playerDir, playerUniqueId + ".dat");
        if (playerFile.exists()) {
            try {
                NbtIo.writeCompressed(playerData, playerFile);
                return true;
            } catch (IOException ignored) {}
        }

        return false;
    }


    public static void setInventoryContents(UUID playerUniqueId, Map<Integer, ItemStack>/* Map<MinecraftInventorySlotIndex, MinecraftItemStack> */ contents) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUniqueId);
        if (player.isOnline()) {
            PlayerInventory playerInventory = player.getPlayer().getInventory();
            playerInventory.clear();
            contents.forEach((slot, itemStack) -> playerInventory.setItem(convInvSlotMinecraft2Bukkit(slot), MinecraftAdapter.ItemStack.itemStack(itemStack)));
        } else {
            CompoundTag data = loadPlayerData(playerUniqueId);
            if (data != null && data.contains("Inventory")) {
                ListTag inventoryList = data.getList("Inventory", Tag.TAG_COMPOUND);
                inventoryList.clear();

                contents.forEach((slot, item) -> {
                    CompoundTag inventoryItemContainer = new CompoundTag();
                    inventoryItemContainer.putInt("Slot", slot);
                    inventoryList.set(slot, item.save(new CompoundTag()));
                });
            }

            savePlayerData(playerUniqueId, data);
        }
    }

    private static int convInvSlotBukkit2Minecraft(int bukkitSlot) {
        return bukkitSlot < 27 ? bukkitSlot + 9 : bukkitSlot < 36 ? bukkitSlot - 27 : bukkitSlot < 40 ? bukkitSlot + 36 : bukkitSlot == 40 ? 150 : 39 - (bukkitSlot - 36);
    }

    private static int convInvSlotMinecraft2Bukkit(int minecraftSlot) {
        return minecraftSlot < 9 ? minecraftSlot + 27 : minecraftSlot < 36 ? minecraftSlot - 9 : minecraftSlot < 100 ? minecraftSlot - 36 : minecraftSlot < 104 ? 39 - (minecraftSlot - 100) : minecraftSlot == 150 ? 40 : minecraftSlot;
    }

    public static class WrappedInventory implements Inventory, Listener {
        private final Inventory originalInventory;
        private final Set<Consumer<InventoryOpenEvent>> onOpen = new HashSet<>();
        private final Set<Consumer<InventoryClickEvent>> onClick = new HashSet<>();
        private final Set<Consumer<InventoryCloseEvent>> onClose = new HashSet<>();

        public WrappedInventory(Inventory original) {
            if (original instanceof WrappedInventory) throw new IllegalArgumentException("Cannot create instance because WrapWrapInventory is not accepted.");
            this.originalInventory = original;
        }

        public Inventory getOriginalInventory() {
            return this.originalInventory;
        }

        public void openInventory(HumanEntity player) {
            player.openInventory(this);
        }

        public WrappedInventory registerListener() {
            ListenerManager.registerListener(this);
            return this;
        }


        public WrappedInventory unregisterListener() {
            ListenerManager.unregisterListener(this);
            return this;
        }

        public WrappedInventory appendOnOpenHandler(Consumer<InventoryOpenEvent> onOpen) {
            this.onOpen.add(onOpen);
            return this;
        }

        public WrappedInventory clearOnOpenHandlers() {
            this.onOpen.clear();
            return this;
        }

        public WrappedInventory appendOnClickHandler(Consumer<InventoryClickEvent> onClick) {
            this.onClick.add(onClick);
            return this;
        }

        public WrappedInventory clearOnClickHandlers() {
            this.onClick.clear();
            return this;
        }

        public WrappedInventory appendOnCloseHandler(Consumer<InventoryCloseEvent> onClose) {
            this.onClose.add(onClose);
            return this;
        }

        public WrappedInventory clearOnCloseHandlers() {
            this.onClose.clear();
            return this;
        }

        @EventHandler
        public void onInventoryOpen(InventoryOpenEvent event) {
            if (event.getInventory().equals(this.originalInventory)) {
                this.onOpen.forEach(handler -> handler.accept(event));
            }
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (event.getClickedInventory() != null && event.getInventory().equals(this.originalInventory)) {
                this.onClick.forEach(handler -> handler.accept(event));
            }
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (event.getInventory().equals(this.originalInventory)) {
                this.onClose.forEach(handler -> handler.accept(event));
            }
        }

        @Override
        public int getSize() {
            return this.originalInventory.getSize();
        }

        @Override
        public int getMaxStackSize() {
            return this.originalInventory.getMaxStackSize();
        }

        @Override
        public void setMaxStackSize(int size) {
            this.originalInventory.setMaxStackSize(size);
        }

        @Override
        public org.bukkit.inventory.@Nullable ItemStack getItem(int index) {
            return this.originalInventory.getItem(index);
        }

        @Override
        public void setItem(int index, org.bukkit.inventory.@Nullable ItemStack item) {
            this.originalInventory.setItem(index, item);
        }

        @Override
        public @NotNull HashMap<Integer, org.bukkit.inventory.ItemStack> addItem(org.bukkit.inventory.@NotNull ItemStack... items) throws IllegalArgumentException {
            return this.originalInventory.addItem(items);
        }

        @Override
        public @NotNull HashMap<Integer, org.bukkit.inventory.ItemStack> removeItem(org.bukkit.inventory.@NotNull ItemStack... items) throws IllegalArgumentException {
            return this.originalInventory.removeItem(items);
        }

        @Override
        public @NotNull HashMap<Integer, org.bukkit.inventory.ItemStack> removeItemAnySlot(org.bukkit.inventory.@NotNull ItemStack... items) throws IllegalArgumentException {
            return this.originalInventory.removeItemAnySlot(items);
        }

        @Override
        public org.bukkit.inventory.@Nullable ItemStack @NotNull [] getContents() {
            return this.originalInventory.getContents();
        }

        @Override
        public void setContents(org.bukkit.inventory.@Nullable ItemStack @NotNull [] items) throws IllegalArgumentException {
            this.originalInventory.setContents(items);
        }

        @Override
        public org.bukkit.inventory.@Nullable ItemStack @NotNull [] getStorageContents() {
            return this.originalInventory.getStorageContents();
        }

        @Override
        public void setStorageContents(org.bukkit.inventory.@Nullable ItemStack @NotNull [] items) throws IllegalArgumentException {
            this.originalInventory.setStorageContents(items);
        }

        @Override
        public boolean contains(@NotNull Material material) throws IllegalArgumentException {
            return this.originalInventory.contains(material);
        }

        @Override
        public boolean contains(org.bukkit.inventory.@Nullable ItemStack item) {
            return this.originalInventory.contains(item);
        }

        @Override
        public boolean contains(@NotNull Material material, int amount) throws IllegalArgumentException {
            return this.originalInventory.contains(material, amount);
        }

        @Override
        public boolean contains(org.bukkit.inventory.@Nullable ItemStack item, int amount) {
            return this.originalInventory.contains(item, amount);
        }

        @Override
        public boolean containsAtLeast(org.bukkit.inventory.@Nullable ItemStack item, int amount) {
            return this.originalInventory.containsAtLeast(item, amount);
        }

        @Override
        public @NotNull HashMap<Integer, ? extends org.bukkit.inventory.ItemStack> all(@NotNull Material material) throws IllegalArgumentException {
            return this.originalInventory.all(material);
        }

        @Override
        public @NotNull HashMap<Integer, ? extends org.bukkit.inventory.ItemStack> all(org.bukkit.inventory.@Nullable ItemStack item) {
            return this.originalInventory.all(item);
        }

        @Override
        public int first(@NotNull Material material) throws IllegalArgumentException {
            return this.originalInventory.first(material);
        }

        @Override
        public int first(org.bukkit.inventory.@NotNull ItemStack item) {
            return this.originalInventory.first(item);
        }

        @Override
        public int firstEmpty() {
            return this.originalInventory.firstEmpty();
        }

        @Override
        public boolean isEmpty() {
            return this.originalInventory.isEmpty();
        }

        @Override
        public void remove(@NotNull Material material) throws IllegalArgumentException {
            this.originalInventory.remove(material);
        }

        @Override
        public void remove(org.bukkit.inventory.@NotNull ItemStack item) {
            this.originalInventory.remove(item);
        }

        @Override
        public void clear(int index) {
            this.originalInventory.clear(index);
        }

        @Override
        public void clear() {
            this.originalInventory.clear();
        }

        @Override
        public int close() {
            return this.originalInventory.close();
        }

        @Override
        public @NotNull List<HumanEntity> getViewers() {
            return this.originalInventory.getViewers();
        }

        @Override
        public @NotNull InventoryType getType() {
            return this.originalInventory.getType();
        }

        @Override
        public @Nullable InventoryHolder getHolder() {
            return this.originalInventory.getHolder();
        }

        @Override
        public @Nullable InventoryHolder getHolder(boolean useSnapshot) {
            return this.originalInventory.getHolder(useSnapshot);
        }

        @Override
        public @NotNull ListIterator<org.bukkit.inventory.ItemStack> iterator() {
            return this.originalInventory.iterator();
        }

        @Override
        public @NotNull ListIterator<org.bukkit.inventory.ItemStack> iterator(int index) {
            return this.originalInventory.iterator(index);
        }

        @Override
        public @Nullable Location getLocation() {
            return this.originalInventory.getLocation();
        }
    }
}
