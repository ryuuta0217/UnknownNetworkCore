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

package net.unknown.core.gui;

import net.kyori.adventure.text.Component;
import net.unknown.UnknownNetworkCore;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GuiBase implements Listener {
    private Component guiTitle;
    protected final Inventory inventory;
    protected boolean unRegisterOnClose;
    protected BiConsumer<Integer, InventoryClickEvent> onClick;

    public GuiBase(InventoryHolder owner, InventoryType type, Component guiTitle, boolean unRegisterOnClose) {
        this.inventory = Bukkit.createInventory(owner, type, guiTitle);
        this.unRegisterOnClose = unRegisterOnClose;
        registerAsListener();
    }

    public GuiBase(InventoryHolder owner, int size, Component guiTitle, boolean unRegisterOnClose) {
        this.inventory = Bukkit.createInventory(owner, size, guiTitle);
        this.unRegisterOnClose = unRegisterOnClose;
        registerAsListener();
    }

    public GuiBase(InventoryHolder owner, InventoryType type, Component guiTitle, Consumer<Inventory> initializer, boolean unRegisterOnClose) {
        this.inventory = Bukkit.createInventory(owner, type, guiTitle);
        this.unRegisterOnClose = unRegisterOnClose;
        registerAsListener();
        initializer.accept(this.inventory);
    }

    public GuiBase(InventoryHolder owner, int size, Component guiTitle, Consumer<Inventory> initializer, boolean unRegisterOnClose) {
        this.inventory = Bukkit.createInventory(owner, size, guiTitle);
        this.unRegisterOnClose = unRegisterOnClose;
        registerAsListener();
        initializer.accept(this.inventory);
    }

    public void onOpen(InventoryOpenEvent event) {}
    public void onClose(InventoryCloseEvent event) {}
    public void onClick(InventoryClickEvent event) {
        if(this.onClick != null) {
            event.setCancelled(true);
            this.onClick.accept(event.getSlot(), event);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if(!event.getInventory().equals(this.inventory)) return;
        onOpen(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if(!event.getInventory().equals(this.inventory)) return;
        onClose(event);
        if(this.unRegisterOnClose) unRegisterAsListener();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(!this.inventory.equals(event.getClickedInventory())) return;
        event.setCancelled(true);
        onClick(event);
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    protected void registerAsListener() {
        Bukkit.getPluginManager().registerEvents(this, UnknownNetworkCore.getInstance());
    }

    protected void unRegisterAsListener() {
        HandlerList.unregisterAll(this);
    }
}
