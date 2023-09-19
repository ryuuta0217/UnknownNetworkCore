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

package net.unknown.core.gui;

import net.kyori.adventure.text.Component;
import net.unknown.core.gui.view.View;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.function.Consumer;

public abstract class ViewedGuiBase<V extends View> extends GuiBase {
    private V prevView;
    private V view;

    public ViewedGuiBase(InventoryHolder owner, InventoryType type, Component guiTitle, boolean unRegisterOnClose, V firstView) {
        super(owner, type, guiTitle, unRegisterOnClose);
        this.view = firstView;
    }

    public ViewedGuiBase(InventoryHolder owner, int size, Component guiTitle, boolean unRegisterOnClose, V firstView) {
        super(owner, size, guiTitle, unRegisterOnClose);
        this.view = firstView;
    }

    public ViewedGuiBase(InventoryHolder owner, InventoryType type, Component guiTitle, Consumer<Inventory> initializer, boolean unRegisterOnClose, V firstView) {
        super(owner, type, guiTitle, unRegisterOnClose);
        this.view = firstView;
    }

    public ViewedGuiBase(InventoryHolder owner, int size, Component guiTitle, Consumer<Inventory> initializer, boolean unRegisterOnClose, V firstView) {
        super(owner, size, guiTitle, unRegisterOnClose);
        this.view = firstView;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        this.view.initialize();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        this.view.onClick(event);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        this.view.onClose(event);
    }

    @Override
    public void onUnregistering() {
        this.view.onUnregistering();
    }

    @Override
    public void onUnregistered() {
        this.view.onUnregistered();
    }

    public V getPreviousView() {
        return this.prevView;
    }

    public V getView() {
        return this.view;
    }

    public void setPreviousView(V prevView) {
        this.prevView = prevView;
    }

    public void setView(V newView) {
        if (this.getView() != null) {
            this.view.clearInventory();
            this.setPreviousView(this.getView());
        }
        this.view = newView;
        this.view.initialize();
    }
}
