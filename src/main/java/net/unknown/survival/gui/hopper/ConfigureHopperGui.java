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

package net.unknown.survival.gui.hopper;

import net.kyori.adventure.text.Component;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.gui.view.View;
import net.unknown.launchwrapper.hopper.IMixinHopperBlockEntity;
import net.unknown.survival.gui.hopper.view.ConfigureHopperView;
import org.bukkit.craftbukkit.v1_19_R2.block.CraftHopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class ConfigureHopperGui extends GuiBase {
    private final HopperBlockEntity hopper;
    private final IMixinHopperBlockEntity mixinHopper;
    private View view = new ConfigureHopperView(this);

    public ConfigureHopperGui(Player opener, HopperBlockEntity hopper, IMixinHopperBlockEntity mixinHopper) {
        super(opener, 54, Component.text("ホッパーの設定", DefinedTextColor.BLUE), true);
        this.hopper = hopper;
        this.mixinHopper = mixinHopper;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        this.view.initialize();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        this.view.onClick(event);
    }

    public HopperBlockEntity getHopper() {
        return this.hopper;
    }

    public IMixinHopperBlockEntity getMixinHopper() {
        return this.mixinHopper;
    }

    public View getView() {
        return this.view;
    }

    public void setView(View newView) {
        this.view.clearInventory();
        this.view = newView;
        this.view.initialize();
    }

    public static class Listener implements org.bukkit.event.Listener {
        @EventHandler
        public void onInteractHopper(PlayerInteractEvent event) {
            if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if (event.getClickedBlock() == null) return;
            if (event.getClickedBlock().getType() != org.bukkit.Material.HOPPER) return;
            if (!event.getPlayer().isSneaking()) return;
            event.setCancelled(true);
            HopperBlockEntity hopper = ((CraftHopper) event.getClickedBlock().getState()).getTileEntity();
            new ConfigureHopperGui(event.getPlayer(), hopper, (IMixinHopperBlockEntity) hopper).open(event.getPlayer());
        }
    }
}
