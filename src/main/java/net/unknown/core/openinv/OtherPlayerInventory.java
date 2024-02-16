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

package net.unknown.core.openinv;

import com.google.common.collect.ImmutableList;
import com.onarandombox.multiverseinventories.MultiverseInventories;
import com.onarandombox.multiverseinventories.profile.PlayerProfile;
import com.onarandombox.multiverseinventories.profile.ProfileTypes;
import com.onarandombox.multiverseinventories.share.Sharables;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.ObfuscationUtil;
import net.unknown.core.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.*;

public class OtherPlayerInventory extends Inventory implements Listener {
    public static final Set<OtherPlayerInventory> INSTANCES = new HashSet<>();

    static {
        RunnableManager.runRepeating(OtherPlayerInventory::gc, 20 * 30, 20 * 30);
    }

    private static void gc() {
        INSTANCES.removeIf(instance -> {
            if (instance.active && !instance.getViewers().isEmpty()) {
                return false;
            } else {
                instance.active = false;
                if (instance.saveTask != null && !instance.saveTask.isCancelled()) instance.saveTask.cancel();
                ListenerManager.unregisterListener(instance);
                //System.out.println(instance.player.getName() + "'s inventory gc() executed. (" + instance + ")");
                return true;
            }
        });
    }

    private boolean active;
    private final CraftInventory bukkit = new CraftInventory(this);
    private net.minecraft.world.entity.player.Player player;
    private boolean online = false;
    private boolean dirty = false;
    private final BukkitTask saveTask;

    // Constructor Boss
    public OtherPlayerInventory(ServerPlayer minecraft) {
        super(minecraft);

        ListenerManager.registerListener(this); // 対象がログイン/ログアウトしたことを検知するイベントハンドラ用

        this.active = true;

        INSTANCES.add(this);
        this.online = minecraft.isRealPlayer;
        this.player = super.player;
        this.selected = this.player.getInventory().selected; // Hotbar selected slot

        // items, armor, offhand, compartmentsをプレイヤーのインベントリのCollectionへの直接参照にする
        NonNullList<ItemStack> items = this.player.getInventory().items;
        NonNullList<ItemStack> armor = this.player.getInventory().armor;
        NonNullList<ItemStack> offhand = this.player.getInventory().offhand;
        ImmutableList<NonNullList<ItemStack>> compartments = ImmutableList.of(items, armor, offhand);

        try {
            ObfuscationUtil.Class obfuscatedInventoryClass = ObfuscationUtil.getClassByName(Inventory.class.getName());
            if (obfuscatedInventoryClass == null) throw new IllegalStateException("What? Inventory is not obfuscated? Class name is " + Inventory.class.getName() + "!"); // If class is not found, throw exception

            ReflectionUtil.setFinalObject(obfuscatedInventoryClass.getFieldByMojangName("items").getField(), this, items);
            ReflectionUtil.setFinalObject(obfuscatedInventoryClass.getFieldByMojangName("armor").getField(), this, armor);
            ReflectionUtil.setFinalObject(obfuscatedInventoryClass.getFieldByMojangName("offhand").getField(), this, offhand);
            ReflectionUtil.setFinalObject(obfuscatedInventoryClass.getFieldByMojangName("compartments").getField(), this, compartments);
        } catch(NoSuchFieldException e) {
            e.printStackTrace(); // ここには到達しないでほしい (お祈り)
        }

        // 毎秒、インベントリが変更されているかをチェックし、変更されていたらセーブする (オフラインのプレイヤーのみ)
        this.saveTask = RunnableManager.runRepeating(() -> {
            if (this.active && this.dirty && !this.online) {
                System.out.println("Background saving " + this.player.getName() + "'s inventory...");
                ((ServerPlayer) this.player).getBukkitEntity().saveData();
                this.dirty = false;
            }
        }, 20, 20);
    }

    public OtherPlayerInventory(Player bukkit) {
        this(MinecraftAdapter.player(bukkit));
    }

    public OtherPlayerInventory(net.minecraft.world.entity.player.Player minecraft) {
        this((Player) minecraft.getBukkitEntity());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 対象がログインした場合
        if (!this.online && event.getPlayer().getUniqueId().equals(this.player.getUUID())) {
            this.dirty = false; // これからオンラインのプレイヤーに対して直接インベントリの変更を書き込むので、データは変更されていないことにする

            ServerPlayer offlinePlayer = (ServerPlayer) this.player; // in DedicatedServer, Player is always ServerPlayer.
            ServerPlayer onlinePlayer = MinecraftAdapter.player(event.getPlayer());
            onlinePlayer.getInventory().transaction.addAll(this.transaction);
            this.player = onlinePlayer;
            Inventory onlineInventory = onlinePlayer.getInventory();

            for (int i = 0; i < this.getContainerSize(); i++) {
                onlineInventory.setItem(i, this.getItem(i));
            }

            onlineInventory.selected = this.selected;
            try {
                ObfuscationUtil.Class obfuscatedInventoryClass = ObfuscationUtil.getClassByName(Inventory.class.getName());
                if (obfuscatedInventoryClass == null) throw new IllegalStateException("What? Inventory is not obfuscated? Class name is " + Inventory.class.getName() + "!"); // If class is not found, throw exception

                ReflectionUtil.setFinalObject(obfuscatedInventoryClass.getFieldByMojangName("items").getField(), this, onlineInventory.items);
                ReflectionUtil.setFinalObject(obfuscatedInventoryClass.getFieldByMojangName("armor").getField(), this, onlineInventory.armor);
                ReflectionUtil.setFinalObject(obfuscatedInventoryClass.getFieldByMojangName("offhand").getField(), this, onlineInventory.offhand);
                ReflectionUtil.setFinalObject(obfuscatedInventoryClass.getFieldByMojangName("compartments").getField(), this, ImmutableList.of(this.items, this.armor, this.offhand));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            Inventory offlineInventory = offlinePlayer.getInventory();
            offlineInventory.transaction.remove(offlinePlayer.getBukkitEntity()); // Remove owner from old inventory viewer (if contains)
            onlineInventory.transaction.addAll(offlineInventory.transaction); // Add viewers to new inventory
            this.online = true;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Only loads from data when player is logged out.
        if (this.online && event.getPlayer().getUniqueId().equals(this.player.getUUID())) {
            this.dirty = false;

            ServerPlayer offlinePlayer = (ServerPlayer) OpenInv.load(event.getPlayer());
            ServerPlayer onlinePlayer = (ServerPlayer) this.player;
            offlinePlayer.getInventory().transaction.addAll(this.transaction);
            this.player = offlinePlayer;
            Inventory offlineInventory = offlinePlayer.getInventory();

            for (int i = 0; i < this.getContainerSize(); i++) {
                offlineInventory.setItem(i, this.getItem(i));
            }

            offlineInventory.selected = this.selected;
            try {
                ObfuscationUtil.Class obfuscatedInventoryClass = ObfuscationUtil.getClassByName(Inventory.class.getName());
                if (obfuscatedInventoryClass == null) throw new IllegalStateException("What? Inventory is not obfuscated? Class name is " + Inventory.class.getName() + "!"); // If class is not found, throw exception

                ReflectionUtil.setFinalObject(obfuscatedInventoryClass.getFieldByMojangName("items").getField(), this, offlineInventory.items);
                ReflectionUtil.setFinalObject(obfuscatedInventoryClass.getFieldByMojangName("armor").getField(), this, offlineInventory.armor);
                ReflectionUtil.setFinalObject(obfuscatedInventoryClass.getFieldByMojangName("offhand").getField(), this, offlineInventory.offhand);
                ReflectionUtil.setFinalObject(obfuscatedInventoryClass.getFieldByMojangName("compartments").getField(), this, ImmutableList.of(this.items, this.armor, this.offhand));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            Inventory onlineInventory = onlinePlayer.getInventory();
            onlineInventory.transaction.remove(onlinePlayer.getBukkitEntity()); // Remove owner from old inventory viewer (if contains)
            offlineInventory.transaction.addAll(onlineInventory.transaction); // Add viewers to new inventory
            this.online = false;
        }
    }

    public CraftInventory getBukkitInventory() {
        return this.bukkit;
    }

    public InventoryView open(Player bukkitPlayer) {
        // TODO: CraftHumanEntity#openCustomInventory の処理を参照する, またはOpenInv/internal/PlayerDataManager#openInventory
        return bukkitPlayer.openInventory(this.bukkit);
    }

    @Override
    public Component getName() {
        return Component.translatable("container.inventory").append(": ").append(this.player.getName());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!this.online) this.dirty = true; // オフラインプレイヤーのインベントリが変更された場合に限り、セーブ用にフラグを立てる
    }

    @Override // 防具とかのスロットを表示するためにサイズを45にする
    public int getContainerSize() {
        return 45;
    }

    private enum ConnectionType {
        DIRECT, MULTIVERSE_INVENTORIES
    }
}
